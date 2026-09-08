package cn.studypilot.retrieval.gateway;

import cn.studypilot.retrieval.dto.RetrievalRequest;
import cn.studypilot.retrieval.dto.RetrievalResponse;

/** 独立检索服务的边界；后续实现负责 HTTP、超时和错误语义。 */
public interface RetrievalGateway { RetrievalResponse retrieve(RetrievalRequest request); }
