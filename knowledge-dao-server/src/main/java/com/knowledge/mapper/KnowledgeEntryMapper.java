package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.common.vo.ContentTypeCountVO;
import com.knowledge.entity.KnowledgeEntryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeEntryMapper extends BaseMapper<KnowledgeEntryEntity> {

    /**
     * Vector similarity search (cosine distance via pgvector).
     */
    @Select("""
        SELECT id, title, content, content_type, tags, source,
               created_at, updated_at, user_id, is_shared,
               (embedding <=> #{queryVector}::vector) AS distance
        FROM knowledge_base
        WHERE user_id = #{userId}::text
        ORDER BY embedding <=> #{queryVector}::vector
        LIMIT #{topK}
        """)
    List<KnowledgeEntryEntity> searchByVector(@Param("queryVector") String queryVector,
                                              @Param("userId") Long userId,
                                              @Param("topK") int topK);

    @Select("UPDATE knowledge_base SET embedding = #{vector}::vector, updated_at = NOW() WHERE id = #{id}")
    void updateEmbedding(@Param("id") Long id, @Param("vector") String vector);

    @Select("SELECT COUNT(*) FROM knowledge_base WHERE user_id = #{userId}::text")
    long countByUserId(@Param("userId") Long userId);

    @Select("SELECT content_type, COUNT(*) FROM knowledge_base WHERE user_id = #{userId}::text GROUP BY content_type")
    List<ContentTypeCountVO> countGroupByContentType(@Param("userId") Long userId);
}
