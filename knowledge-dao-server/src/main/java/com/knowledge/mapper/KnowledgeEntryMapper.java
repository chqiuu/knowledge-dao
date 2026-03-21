package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.entity.KnowledgeEntryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeEntryMapper extends BaseMapper<KnowledgeEntryEntity> {

    /**
     * Vector similarity search (cosine distance via pgvector).
     * Results sorted by distance (ascending = most similar).
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

    /**
     * Update embedding vector for a given entry.
     */
    @Select("UPDATE knowledge_base SET embedding = #{vector}::vector, updated_at = NOW() WHERE id = #{id}")
    void updateEmbedding(@Param("id") Long id, @Param("vector") String vector);

    /**
     * Count entries by user.
     */
    @Select("SELECT COUNT(*) FROM knowledge_base WHERE user_id = #{userId}::text")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Count by content type for a user.
     */
    @Select("SELECT content_type, COUNT(*) FROM knowledge_base WHERE user_id = #{userId}::text GROUP BY content_type")
    List<ContentTypeCount> countGroupByContentType(@Param("userId") Long userId);

    interface ContentTypeCount {
        String getContentType();
        Long getCount();
    }
}
