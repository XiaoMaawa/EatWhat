package com.eatwhat.common.exception;

import com.eatwhat.common.result.R;
import com.eatwhat.security.PublicApi;
import com.eatwhat.security.RequireRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅供 Web 层测试使用的探针控制器。
 *
 * <p>覆盖业务异常、参数校验、类型转换、路径不存在、方法不支持、角色越权与正常响应
 * 等场景，用于验证 {@link GlobalExceptionHandler} 的 HTTP 状态码映射。</p>
 */
@RestController
@RequestMapping("/api/v1/probe")
public class ProbeController {

    /** 正常响应 */
    @PublicApi
    @GetMapping("/ok")
    public R<String> ok() {
        return R.ok("pong");
    }

    /** 抛出业务异常 */
    @PublicApi
    @GetMapping("/biz")
    public R<Void> biz() {
        throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH);
    }

    /** 抛出带自定义消息的业务异常 */
    @PublicApi
    @GetMapping("/biz-custom")
    public R<Void> bizCustom() {
        throw BizException.of(ErrorCode.PARAM_INVALID, "自定义提示信息");
    }

    /** 请求体校验失败 */
    @PublicApi
    @PostMapping("/validate")
    public R<Void> validate(@Valid @RequestBody ProbeRequest request) {
        return R.ok();
    }

    /** 缺少必填查询参数 */
    @PublicApi
    @GetMapping("/require-param")
    public R<String> requireParam(@RequestParam("kw") String keyword) {
        return R.ok(keyword);
    }

    /** 路径变量类型转换失败 */
    @PublicApi
    @GetMapping("/number/{id}")
    public R<Long> number(@PathVariable("id") Long id) {
        return R.ok(id);
    }

    /** 仅商家可访问 */
    @RequireRole("MERCHANT")
    @GetMapping("/merchant-only")
    public R<String> merchantOnly() {
        return R.ok("merchant");
    }

    /** 未登录也可访问，但登录后校验角色 */
    @PublicApi
    @RequireRole("ADMIN")
    @GetMapping("/admin-only-public")
    public R<String> adminOnlyPublic() {
        return R.ok("admin");
    }

    /** 需要登录 */
    @GetMapping("/private")
    public R<String> privateApi() {
        return R.ok("private");
    }

    /** 校验请求体 */
    public static class ProbeRequest {

        @NotBlank(message = "名称不能为空")
        private String name;

        @Min(value = 1, message = "数量至少为 1")
        private Integer quantity;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
