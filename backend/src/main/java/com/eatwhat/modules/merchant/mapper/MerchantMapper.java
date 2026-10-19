package com.eatwhat.modules.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家信息 Mapper。
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
