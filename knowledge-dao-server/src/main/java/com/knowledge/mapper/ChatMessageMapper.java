package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.common.vo.HotQueryVO;
import com.knowledge.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {

    @Select("SELECT * FROM chat_messages WHERE user_id = #{userId}::text AND role = 'user' ORDER BY created_at DESC LIMIT #{limit}")
    List<ChatMessageEntity> findUserQueries(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM chat_messages WHERE role = 'user' ORDER BY created_at DESC LIMIT #{limit}")
    List<ChatMessageEntity> findAllQueries(@Param("limit") int limit);

    @Select("SELECT content AS query, COUNT(*) AS cnt FROM chat_messages WHERE role = 'user' GROUP BY content ORDER BY cnt DESC LIMIT #{limit}")
    List<HotQueryVO> findHotQueries(@Param("limit") int limit);
}
