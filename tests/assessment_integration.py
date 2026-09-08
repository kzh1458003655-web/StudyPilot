"""Run the exam/assessment path against an already started local StudyPilot service."""
import json
import pathlib
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:18080/api"

def request(path, body=None, method=None):
    data = json.dumps(body, ensure_ascii=False).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)

def main():
    # 新课程保证运行时测试不会污染用户现有课程，并直接验证课程隔离。
    course = request("/courses", {"name": "评测流程自动化测试"}, "POST")
    course_id = course["id"]
    try:
        seeded = request("/assessment/seed", {"courseId": course_id}, "POST")
        assert seeded["questions"] == 5
        topics = request("/assessment/topics?courseId=" + course_id)
        assert len(topics) == 4 and topics[0]["frequency"] >= topics[-1]["frequency"]
        paper = request("/assessment/papers", {"courseId": course_id, "count": 5}, "POST")
        assert len(paper["questions"]) == 5
        # 试卷响应不含参考答案；这项断言防止未来改动意外泄漏答案。
        assert all("answer_key" not in question and "rubric" not in question for question in paper["questions"])
        answers = []
        for question in paper["questions"]:
            text = "利用原子性与一致性，使计划和任务全部成功；出现异常时全部回滚。"
            if question["type"] == "choice":
                # 此处只验证接口闭环，随意选择一个合法选项即可。
                text = "A"
            answers.append({"question_id": question["id"], "answer": text})
        attempt = request("/assessment/papers/" + paper["id"] + "/submit",
                          {"courseId": course_id, "answers": answers}, "POST")
        assert attempt["total_score"] == 100 and len(attempt["results"]) == 5
        attempts = request("/assessment/attempts?courseId=" + course_id)
        assert len(attempts) == 1
        try:
            request("/assessment/papers/" + paper["id"] + "?courseId=00000000-0000-0000-0000-000000000001")
            raise AssertionError("cross-course request should fail")
        except urllib.error.HTTPError as error:
            assert error.code == 404
        print("PASS assessment integration: seed → topic statistics → paper → server scoring → history")
    finally:
        request("/courses/" + course_id, method="DELETE")

if __name__ == "__main__":
    main()
