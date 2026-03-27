package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.controller.dto.*;
import com.knowledge.service.KnowledgeService;
import com.knowledge.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RAG API", description = "RAG 相关接口")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final KnowledgeService knowledgeService;
    private final RagService ragService;

    @PostMapping("/insert")
    @Operation(summary = "插入知识条目")
    public Result<?> insert(@Valid @RequestBody InsertReq req) {
        return Result.ok(knowledgeService.insert(req), "知识条目插入成功");
    }

    @PostMapping("/search")
    @Operation(summary = "语义搜索")
    public Result<?> search(@Valid @RequestBody SearchReq req) {
        int topK = req.getTopK() != null && req.getTopK() > 0 ? req.getTopK() : 5;
        return Result.ok(knowledgeService.search(req.getQuery(), req.getUserId(), topK), "找到 " + topK + " 条结果");
    }

    @PostMapping("/chat")
    @Operation(summary = "RAG 聊天")
    public Result<?> chat(@Valid @RequestBody ChatReq req) {
        return Result.ok(ragService.chat(req), "RAG 聊天完成");
    }

    @GetMapping("/stats")
    @Operation(summary = "用户统计")
    public Result<?> stats(@RequestParam(name = "userId") Long userId) {
        return Result.ok(knowledgeService.getStats(userId));
    }
}
