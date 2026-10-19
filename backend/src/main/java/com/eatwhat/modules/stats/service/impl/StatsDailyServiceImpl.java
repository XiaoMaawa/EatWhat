package com.eatwhat.modules.stats.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.stats.entity.StatsDaily;
import com.eatwhat.modules.stats.mapper.StatsDailyMapper;
import com.eatwhat.modules.stats.service.StatsDailyService;
import com.eatwhat.modules.user.entity.UserAddress;
import com.eatwhat.modules.user.mapper.UserAddressMapper;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.mapper.RiderMapper;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.mapper.MerchantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日统计汇总（公共-09）：定时任务每日 00:05 汇总前一日数据；也支持手动补跑任意日期。
 */
@Service
public class StatsDailyServiceImpl implements StatsDailyService {

    private static final Logger log = LoggerFactory.getLogger(StatsDailyServiceImpl.class);

    private final StatsDailyMapper statsDailyMapper;
    private final OrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final RiderMapper riderMapper;

    public StatsDailyServiceImpl(StatsDailyMapper statsDailyMapper,
                                 OrderMapper orderMapper,
                                 SysUserMapper userMapper,
                                 MerchantMapper merchantMapper,
                                 RiderMapper riderMapper) {
        this.statsDailyMapper = statsDailyMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.riderMapper = riderMapper;
    }

    @Override
    public PageResult<StatsDaily> page(String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<StatsDaily> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(StatsDaily::getStatDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(StatsDaily::getStatDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(StatsDaily::getStatDate);
        Page<StatsDaily> result = statsDailyMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return PageResult.of(result);
    }

    @Override
    public List<StatsDaily> listRange(String startDate, String endDate) {
        LambdaQueryWrapper<StatsDaily> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(StatsDaily::getStatDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(StatsDaily::getStatDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(StatsDaily::getStatDate);
        return statsDailyMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StatsDaily summarize(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        // 幂等：先删旧记录再插新记录
        statsDailyMapper.delete(new LambdaQueryWrapper<StatsDaily>()
                .eq(StatsDaily::getStatDate, date));

        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, dayStart)
                .lt(Order::getCreatedAt, dayEnd));

        int orderCount = orders.size();
        int completed = (int) orders.stream()
                .filter(o -> Integer.valueOf(CommonConstants.ORDER_COMPLETED).equals(o.getStatus()))
                .count();
        int cancelled = (int) orders.stream()
                .filter(o -> Integer.valueOf(CommonConstants.ORDER_CANCELLED).equals(o.getStatus())
                        || Integer.valueOf(CommonConstants.ORDER_REFUNDING).equals(o.getStatus()))
                .count();
        BigDecimal totalAmount = orders.stream()
                .filter(o -> o.getPayTime() != null)
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = orders.stream()
                .filter(o -> Integer.valueOf(CommonConstants.ORDER_REFUNDING).equals(o.getStatus()))
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 新增用户/商家/骑手按创建时间统计（角色角色均可注册）
        Long newUsers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "CONSUMER")
                .ge(SysUser::getCreatedAt, dayStart)
                .lt(SysUser::getCreatedAt, dayEnd));
        Long newMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .ge(Merchant::getCreatedAt, dayStart)
                .lt(Merchant::getCreatedAt, dayEnd));
        Long newRiders = riderMapper.selectCount(new LambdaQueryWrapper<Rider>()
                .ge(Rider::getCreatedAt, dayStart)
                .lt(Rider::getCreatedAt, dayEnd));

        StatsDaily stats = new StatsDaily();
        stats.setStatDate(date);
        stats.setOrderCount(orderCount);
        stats.setCompletedCount(completed);
        stats.setCancelCount(cancelled);
        stats.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        stats.setRefundAmount(refundAmount.setScale(2, RoundingMode.HALF_UP));
        stats.setNewUsers(newUsers.intValue());
        stats.setNewMerchants(newMerchants.intValue());
        stats.setNewRiders(newRiders.intValue());
        statsDailyMapper.insert(stats);
        log.info("每日统计汇总完成 date={} orders={} amount={}", date, orderCount, stats.getTotalAmount());
        return stats;
    }
}