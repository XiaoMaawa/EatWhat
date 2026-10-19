package com.eatwhat.modules.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.delivery.entity.DeliveryTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 配送任务 Mapper。抢单使用条件更新保证并发安全。
 */
@Mapper
public interface DeliveryTaskMapper extends BaseMapper<DeliveryTask> {

    /**
     * 原子抢单：仅当任务仍处于待抢单状态时绑定骑手，返回受影响行数。
     */
    @Update("UPDATE delivery_task SET rider_id = #{riderId}, status = 1, grabbed_at = NOW(), "
            + "updated_at = NOW(), version = version + 1 "
            + "WHERE id = #{taskId} AND status = 0 AND rider_id IS NULL")
    int grab(@Param("taskId") Long taskId, @Param("riderId") Long riderId);
}
