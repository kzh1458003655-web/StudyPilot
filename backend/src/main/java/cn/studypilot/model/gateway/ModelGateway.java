package cn.studypilot.model.gateway;

import cn.studypilot.model.dto.ModelRequest;
import cn.studypilot.model.dto.ModelResponse;

/** 本地模型服务的后端抽象；实现由 ARCH-004 提供，禁止在业务模块中直连端口。 */
public interface ModelGateway { ModelResponse complete(ModelRequest request); }
