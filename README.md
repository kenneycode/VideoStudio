# VideoStudio

`VideoStudio`是一个`Android`上的视频处理库，目前支持视频的编码和解码，具体可以查看工程里的`demo`代码，`demo`中的渲染部分使用了[fusion](https://github.com/kenneycode/fusion)库。


`VideoStudio`的引入方法：

在根`gradle`中添加：

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

要引入的`module`中添加：

```
dependencies {
   implementation 'com.github.VideoStudio:fusion:Tag'
}
```

