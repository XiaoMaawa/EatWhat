package com.eatwhat.modules.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.delivery.entity.Rider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 骑手 Mapper。
 */
@Mapper
public interface RiderMapper extends BaseMapper<Rider> {

    /**
     * 累加骑手完成订单数与累计收益。
     */
    @Update("UPDATE rider SET total_orders = total_orders + 1, total_income = total_income + #{fee}, "
            + "updated_at = NOW() WHERE id = #{riderId} AND deleted = 0")
    int addIncome(@Param("riderId") Long riderId, @Param("fee") BigDecimal fee);
}
