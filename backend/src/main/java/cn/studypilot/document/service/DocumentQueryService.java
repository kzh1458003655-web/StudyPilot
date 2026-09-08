package cn.studypilot.document.service;

import java.util.List;

/** 供 qa、exam、assessment 查询已校验资料的公开边界，不暴露 Repository。 */
public interface DocumentQueryService { List<String> availableDocumentIds(String projectId); }
