package com.eatwhat.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 哈希生成工具（开发期用）。
 *
 * <p>用途：{@code sql/init.sql} 里的账号密码是明文约定，入库必须写 BCrypt 哈希。
 * 运行本类可打印出各账号对应的哈希，替换脚本中的 {@code __HASH_*__} 占位符。</p>
 *
 * <pre>
 * cd backend
 * mvn -q compile exec:java -Dexec.mainClass=com.eatwhat.common.utils.BcryptGen
 * </pre>
 */
public final class BcryptGen {

    private BcryptGen() {
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String[][] accounts = {
                {"ADMIN", "Admin@123"},
                {"USER", "User@123"},
                {"MERCHANT", "Merchant@123"},
                {"RIDER", "Rider@123"},
        };
        for (String[] account : accounts) {
            String hash = encoder.encode(account[1]);
            if (!encoder.matches(account[1], hash)) {
                throw new IllegalStateException("哈希自校验失败：" + account[0]);
            }
            System.out.printf("__HASH_%s__ = %s%n", account[0], hash);
        }
    }
}
