#define CPPHTTPLIB_THREAD_POOL_COUNT 8
#include <atomic>
#include <chrono>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <mutex>
#include <shared_mutex>
#include <unordered_map>
#include <unordered_set>

#include "httplib.h"
#include "json.hpp"
using json = nlohmann::json;
namespace fs = std::filesystem;

// Chunk 是检索的最小证据单元。保留文档、页码和原文，后端才能把模型回答映射回可见来源。
struct Chunk {
  std::string doc, name, text;
  int page;
  std::unordered_map<std::string, int> terms;
  int length;
};
std::vector<Chunk> chunks;
// 检索允许多读并发；文档增删时独占索引。模型推理独占 GPU，防止 8GB 显存下多请求抢占。
std::shared_mutex index_mutex;
std::mutex generation_mutex;
std::atomic<int> pending{0};
std::atomic<long> completed{0};
fs::path index_dir = "data/index";
void authorize(httplib::Client &client) {
  // 模型密钥只由内部网关添加，绝不下发到浏览器或写入索引文件。
  const char *key = std::getenv("STUDY_MODEL_KEY");
  if (key && *key)
    client.set_default_headers(
        {{"Authorization", std::string("Bearer ") + key}});
}

// 按完整 UTF-8 字符切分。中文同时保留单字和相邻二元词，英文按字母数字词切分，
// 这是不依赖分词模型却可用于课程资料检索的折中做法。
std::vector<std::string> tokenize(const std::string &text) {
  std::vector<std::string> result;
  std::string latin, previous;
  auto flush = [&]() {
    if (!latin.empty()) {
      result.push_back(latin);
      latin.clear();
    }
  };
  for (size_t i = 0; i < text.size();) {
    unsigned char c = text[i];
    if (c < 128) {
      previous.clear();
      if (std::isalnum(c))
        latin += std::tolower(c);
      else
        flush();
      ++i;
    } else {
      flush();
      size_t n = (c >= 0xF0 ? 4 : c >= 0xE0 ? 3 : 2);
      if (i + n > text.size()) break;
      std::string character = text.substr(i, n);
      if (n == 3) {
        result.push_back(character);
        if (!previous.empty()) result.push_back(previous + character);
        previous = character;
      } else
        previous.clear();
      i += n;
    }
  }
  flush();
  return result;
}

void add_document(const json &doc) {
  const auto id = doc.at("id").get<std::string>();
  chunks.erase(std::remove_if(chunks.begin(), chunks.end(),
                              [&](const Chunk &c) { return c.doc == id; }),
               chunks.end());
  for (const auto &page : doc.at("pages")) {
    std::string text = page.at("text");
    // 每块约 1800 字节并保留 300 字节重叠；先跳过续字节，不能从 UTF-8 字符中间截断。
    for (size_t start = 0; start < text.size();) {
      size_t end = std::min(start + 1800, text.size());
      while (end < text.size() &&
             (static_cast<unsigned char>(text[end]) & 0xC0) == 0x80)
        ++end;
      Chunk c{id,
              doc.at("name"),
              text.substr(start, end - start),
              page.at("page"),
              {},
              0};
      for (const auto &term : tokenize(c.text)) {
        ++c.terms[term];
        ++c.length;
      }
      if (c.length) chunks.push_back(std::move(c));
      if (end == text.size()) break;
      start = end > 300 ? end - 300 : end;
      while (start < text.size() &&
             (static_cast<unsigned char>(text[start]) & 0xC0) == 0x80)
        ++start;
    }
  }
}

json retrieve(const std::string &query, int limit, const json &allowed) {
  std::shared_lock lock(index_mutex);
  std::unordered_set<std::string> ids;
  for(const auto &id:allowed) ids.insert(id.get<std::string>());
  auto terms = tokenize(query);
  std::unordered_set<std::string> unique(terms.begin(), terms.end());
  double average = 0;
  size_t scoped_count=0;
  for (const auto &c : chunks) if(ids.count(c.doc)) { average += c.length; ++scoped_count; }
  average = scoped_count == 0 ? 1 : average / scoped_count;
  std::unordered_map<std::string, int> frequency;
  // 文档频率决定 IDF：只在少数块出现的课程术语更有区分度。
  for (const auto &term : unique)
    for (const auto &c : chunks)
      if (ids.count(c.doc) && c.terms.count(term)) ++frequency[term];
  std::vector<std::pair<double, size_t>> scores;
  for (size_t i = 0; i < chunks.size(); ++i) {
    double score = 0;
    const auto &c = chunks[i];
    if (!ids.count(c.doc)) continue; // 只允许检索 Java 已核验属于当前课程的文档。
    for (const auto &term : unique) {
      auto it = c.terms.find(term);
      if (it == c.terms.end()) continue;
      double idf = std::log(1 + (scoped_count - frequency[term] + 0.5) /
                                    (frequency[term] + 0.5));
      double tf = it->second;
      // BM25：tf 让重复词有收益但逐渐饱和，length/average 抑制长页面天然占优。
      score +=
          idf * (tf * 2.2) / (tf + 1.2 * (0.25 + 0.75 * c.length / average));
    }
    if (score > 0) scores.emplace_back(score, i);
  }
  std::sort(scores.rbegin(), scores.rend());
  json hits = json::array();
  for (int i = 0; i < std::min(limit, static_cast<int>(scores.size())); ++i) {
    const auto &c = chunks[scores[i].second];
    hits.push_back({{"document_id", c.doc},
                    {"name", c.name},
                    {"page", c.page},
                    {"text", c.text},
                    {"score", scores[i].first}});
  }
  return hits;
}

bool valid_id(const std::string &id) {
  // 索引文件名由 UUID 决定，先校验可避免请求路径影响本地文件名。
  return id.size() == 36 && std::all_of(id.begin(), id.end(), [](char c) {
           return std::isxdigit(static_cast<unsigned char>(c)) || c == '-';
         });
}
void reply(httplib::Response &res, const json &body, int code = 200) {
  res.status = code;
  res.set_content(body.dump(), "application/json; charset=utf-8");
}

int main(int argc, char **argv) {
  if (argc > 1) index_dir = argv[1];
  fs::create_directories(index_dir);
  for (const auto &entry : fs::directory_iterator(index_dir))
    if (entry.path().extension() == ".json") {
      try {
        std::ifstream f(entry.path());
        json d;
        f >> d;
        add_document(d);
      } catch (const std::exception &e) {
        std::cerr << "Index read: " << e.what() << std::endl;
      }
    }
  httplib::Server server;
  // 该端口是 Java 的内部依赖。浏览器请求带 Origin，出现时一律拒绝，
  // 使前端不能绕开业务层直接调用模型或检索服务。
  server.set_pre_routing_handler([](const auto &req, auto &res) {
    if (req.has_header("Origin")) {
      reply(res,
            {{"error",
              "Browser origin is not allowed on the internal AI service"}},
            403);
      return httplib::Server::HandlerResponse::Handled;
    }
    return httplib::Server::HandlerResponse::Unhandled;
  });
  server.set_payload_max_length(32 * 1024 * 1024);
  server.set_read_timeout(30);
  server.set_write_timeout(300);
  server.set_exception_handler(
      [](const auto &, auto &res, std::exception_ptr ep) {
        try {
          std::rethrow_exception(ep);
        } catch (const std::exception &e) {
          std::cerr << e.what() << std::endl;
          reply(res, {{"error", "AI service rejected request"}}, 400);
        }
      });
  server.Get("/health", [](const auto &, auto &res) {
    httplib::Client llm("127.0.0.1", 18082);
    llm.set_connection_timeout(2);
    llm.set_read_timeout(2);
    auto status = llm.Get("/health");
    std::shared_lock lock(index_mutex);
    reply(res, {{"service", "StudyPilot C++"},
                {"model_ready", status && status->status == 200},
                {"chunks", chunks.size()},
                {"pending", pending.load()},
                {"completed", completed.load()}});
  });
  server.Post("/documents", [](const auto &req, auto &res) {
    auto d = json::parse(req.body);
    std::string id = d.at("id");
    if (!valid_id(id)) return reply(res, {{"error", "invalid id"}}, 400);
    // 先写临时文件再改名，进程异常时不会留下被半写入的正式索引。
    std::unique_lock lock(index_mutex);
    auto temp = index_dir / (id + ".tmp"), target = index_dir / (id + ".json");
    {
      std::ofstream file(temp);
      file << d.dump();
      if (!file) throw std::runtime_error("Index write failed");
    }
    if (fs::exists(target)) fs::remove(target);
    fs::rename(temp, target);
    add_document(d);
    reply(res, {{"indexed", true}});
  });
  server.Delete(R"(/documents/([a-fA-F0-9-]{36}))", [](const auto &req,
                                                       auto &res) {
    std::string id = req.matches[1];
    std::unique_lock lock(index_mutex);
    fs::remove(index_dir / (id + ".json"));
    chunks.erase(std::remove_if(chunks.begin(), chunks.end(),
                                [&](const Chunk &c) { return c.doc == id; }),
                 chunks.end());
    reply(res, {{"deleted", true}});
  });
  server.Post("/retrieve", [](const auto &req, auto &res) {
    // 返回原文证据而不是直接回答；回答由 Java 结合会话记录和引用保存流程生成。
    auto d = json::parse(req.body);
    reply(res, {{"hits", retrieve(d.at("query"),
                                  std::clamp(d.value("limit", 5), 1, 8), d.value("document_ids", json::array()))}});
  });
  server.Post("/outline", [](const auto &req, auto &res) {
    auto input=json::parse(req.body);
    std::unordered_set<std::string> ids;
    for(const auto &id:input.value("document_ids",json::array())) ids.insert(id.get<std::string>());
    std::shared_lock lock(index_mutex);
    json output = json::array();
    std::unordered_set<std::string> pages;
    for (const auto &c : chunks)
      if (ids.count(c.doc) && pages.insert(c.doc + ":" + std::to_string(c.page)).second &&
          output.size() < 24)
        output.push_back(
            {{"name", c.name}, {"page", c.page}, {"text", c.text}});
    reply(res, {{"outline", output}});
  });
  server.Post("/completion", [](const auto &req, auto &res) {
    if (pending.fetch_add(1) >= 4) {
      --pending;
      return reply(res, {{"error", "模型队列已满，请稍后重试"}}, 429);
    }
    struct Guard {
      ~Guard() { --pending; }
    } guard;
    // pending 限制排队长度，generation_mutex 则确保真正推理一次只占用一份 GPU 上下文。
    std::unique_lock generation(generation_mutex);
    auto body = json::parse(req.body);
    body["stream"] = false;
    body["chat_template_kwargs"] = {{"enable_thinking", false}};
    httplib::Client llm("127.0.0.1", 18082);
    llm.set_read_timeout(180);
    llm.set_connection_timeout(5);
    authorize(llm);
    auto result =
        llm.Post("/v1/chat/completions", body.dump(), "application/json");
    if (!result)
      return reply(res, {{"error", "本地模型未就绪或生成超时"}}, 503);
    res.status = result->status;
    res.set_content(result->body, "application/json; charset=utf-8");
    ++completed;
  });
  server.Post("/stream", [](const auto &req, auto &res) {
    auto body = json::parse(req.body);
    body["stream"] = true;
    body["chat_template_kwargs"] = {{"enable_thinking", false}};
    if (pending.fetch_add(1) >= 4) {
      --pending;
      return reply(res, {{"error", "模型队列已满"}}, 429);
    }
    auto counted = std::make_shared<bool>(true);
    // 以 chunked SSE 透传模型 token；不聚合完整回答，因此浏览器能尽早看到首个字。
    res.set_chunked_content_provider(
        "text/event-stream",
        [body](size_t, httplib::DataSink &sink) {
          std::unique_lock generation(generation_mutex);
          if (!sink.is_writable()) {
            sink.done();
            return true;
          }
          httplib::Client llm("127.0.0.1", 18082);
          llm.set_read_timeout(180);
          llm.set_connection_timeout(5);
          httplib::Request request;
          request.method = "POST";
          request.path = "/v1/chat/completions";
          const char *key = std::getenv("STUDY_MODEL_KEY");
          if (key && *key)
            request.set_header("Authorization", std::string("Bearer ") + key);
          request.set_header("Content-Type", "application/json");
          request.body = body.dump();
          request.content_receiver = [&](const char *data, size_t size,
                                         uint64_t, uint64_t) {
            return sink.is_writable() && sink.write(data, size);
          };
          httplib::Response response;
          httplib::Error error;
          bool ok = llm.send(request, response, error);
          if ((!ok || response.status != 200) && sink.is_writable()) {
            const std::string failure =
                "event: error\ndata: "
                "{\"error\":\"模型连接失败或生成中断\"}\n\n";
            sink.write(failure.data(), failure.size());
          }
          ++completed;
          sink.done();
          return true;
        },
        [counted](bool) {
          if (*counted) {
            --pending;
            *counted = false;
          }
        });
  });
  std::cout << "StudyPilot AI listening on 127.0.0.1:18081" << std::endl;
  return server.listen("127.0.0.1", 18081) ? 0 : 1;
}
