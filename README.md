# 自制卡牌工坊 Card Workshop

Android 卡牌编辑器第一版工程。

## 在线构建 APK

1. 将整个项目文件夹上传到 GitHub 仓库。
2. 打开仓库的 Actions 页面。
3. 选择 `Build Card Workshop APK`。
4. 点击 `Run workflow`。
5. 构建完成后，在本次运行的 Artifacts 中下载 `CardWorkshop-APK`。
6. 解压后安装 `app-debug.apk`。

## 当前版本功能
- 卡牌预览
- 编辑名称、介绍和两个标签
- 修改文字颜色与字号
- 拖动图标、名称、介绍调整位置
- 导出 PNG 到手机相册
- 导入素材文件选择入口（当前版本尚未将所选图片绘制进卡面）

## 字体与素材
将字体放入 `app/src/main/assets/fonts/`。
当前工程尚未自动集成上传的字体和素材压缩包；需要将对应文件放入项目并完成图片渲染接入。

## 构建说明
本工程是可继续开发的初始版本，不代表完整功能已经实现。正式使用前建议先通过 GitHub Actions 构建验证。
