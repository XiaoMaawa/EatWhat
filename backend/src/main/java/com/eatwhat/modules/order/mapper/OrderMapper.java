package com.eatwhat.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper。统计类查询使用原生 SQL 聚合。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 商家在指定时间区间内的有效订单数量与金额（排除待支付、已取消）。
     */
    @Select("SELECT COUNT(*) AS orderCount, IFNULL(SUM(total_amount), 0) AS totalAmount "
            + "FROM orders WHERE merchant_id = #{merchantId} AND deleted = 0 "
            + "AND status NOT IN (0, 6) AND created_at >= #{start} AND created_at < #{end}")
    Map<String, Object> sumByMerchantAndRange(@Param("merchantId") Long merchantId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    /**
     * 商家热销菜品 Top N。
     */
    @Select("SELECT oi.dish_id AS dishId, oi.dish_name AS dishName, SUM(oi.quantity) AS sales "
            + "FROM order_item oi JOIN orders o ON oi.order_id = o.id "
            + "WHERE o.merchant_id = #{merchantId} AND o.deleted = 0 AND o.status NOT IN (0, 6) "
            + "AND o.created_at >= #{start} AND o.created_at < #{end} "
            + "GROUP BY oi.dish_id, oi.dish_name ORDER BY sales DESC LIMIT #{limit}")
    List<Map<String, Object>> topDishes(@Param("merchantId") Long merchantId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("limit") int limit);

    /**
     * 平台订单趋势：按天聚合。
     */
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS count, IFNULL(SUM(total_amount), 0) AS amount "
            + "FROM orders WHERE deleted = 0 AND status NOT IN (0, 6) "
            + "AND created_at >= #{start} GROUP BY DATE(created_at) ORDER BY DATE(created_at)")
    List<Map<String, Object>> dailyTrend(@Param("start") LocalDateTime start);

    /**
     * 平台汇总：区间内订单数、成交额。
     */
    @Select("SELECT COUNT(*) AS orderCount, IFNULL(SUM(total_amount), 0) AS totalAmount "
            + "FROM orders WHERE deleted = 0 AND status NOT IN (0, 6) "
            + "AND created_at >= #{start} AND created_at < #{end}")
    Map<String, Object> sumByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 平台区间统计：有效单量、成交额、完成单量，用于统计看板与环比。
     */
    @Select("SELECT COUNT(*) AS orderCount, IFNULL(SUM(total_amount), 0) AS totalAmount, "
            + "IFNULL(SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END), 0) AS completedCount "
            + "FROM orders WHERE deleted = 0 AND status NOT IN (0, 6) "
            + "AND created_at >= #{start} AND created_at < #{end}")
    Map<String, Object> statsByRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 商家交易额排行 TOP N。
     */
    @Select("SELECT o.merchant_id AS merchantId, s.name AS shopName, COUNT(*) AS orderCount, "
            + "IFNULL(SUM(o.total_amount), 0) AS amount "
            + "FROM orders o LEFT JOIN shop s ON o.shop_id = s.id "
            + "WHERE o.deleted = 0 AND o.status NOT IN (0, 6) "
            + "AND o.created_at >= #{start} AND o.created_at < #{end} "
            + "GROUP BY o.merchant_id, s.name ORDER BY amount DESC LIMIT #{limit}")
    List<Map<String, Object>> topMerchants(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("limit") int limit);

    /**
     * 新增用户统计：按注册日期聚合。
     */
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS count FROM sys_user "
            + "WHERE deleted = 0 AND role = #{role} AND created_at >= #{start} "
            + "GROUP BY DATE(created_at) ORDER BY DATE(created_at)")
    List<Map<String, Object>> dailyNewUsers(@Param("role") String role, @Param("start") LocalDateTime start);

    /**
     * 超时未支付的订单（定时任务自动取消）。expire_at 为业务计算值：created_at + 15 分钟。
     */
    @Select("SELECT * FROM orders WHERE deleted = 0 AND status = 0 "
            + "AND created_at < #{deadline} LIMIT #{limit}")
    List<Order> selectTimeoutOrders(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

    /**
     * 骑手在时间区间内已完成订单的收入合计。
     */
    @Select("SELECT IFNULL(SUM(delivery_fee), 0) FROM orders WHERE rider_id = #{riderId} "
            + "AND status = 5 AND deleted = 0 AND completed_at >= #{start} AND completed_at < #{end}")
    BigDecimal sumRiderIncome(@Param("riderId") Long riderId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /**
     * 骑手在时间区间内已完成订单数量。
     */
    @Select("SELECT COUNT(*) FROM orders WHERE rider_id = #{riderId} AND status = 5 AND deleted = 0 "
            + "AND completed_at >= #{start} AND completed_at < #{end}")
    int countRiderOrders(@Param("riderId") Long riderId,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);
}
