/**
 * commitlint 配置
 * 校验提交信息是否符合 Conventional Commits 规范
 * 详见 docs/08-Git协作规范.md
 */
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'docs',
        'style',
        'refactor',
        'perf',
        'test',
        'build',
        'ci',
        'chore',
        'revert'
      ]
    ],
    'scope-enum': [
      1,
      'always',
      [
        'auth',
        'user',
        'shop',
        'dish',
        'cart',
        'order',
        'delivery',
        'review',
        'admin',
        'common',
        'db',
        'docs',
        'deps',
        'config',
        'ci',
        'srs',
        'hld',
        'dld',
        'dbd',
        'api',
        'test',
        'plan',
        'git',
        'readme'
      ]
    ],
    'subject-case': [0],
    'subject-max-length': [2, 'always', 72],
    'subject-full-stop': [2, 'never', '.'],
    'body-max-line-length': [2, 'always', 100],
    'header-max-length': [2, 'always', 100]
  }
}
