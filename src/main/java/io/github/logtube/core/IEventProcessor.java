package io.github.logtube.core;

import io.github.logtube.utils.ILifeCycle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 处理器暴露的接口，跟日志器负责存储 主机名，项目名，环境名 和 当前线程的 CRID，负责产生具有有效 commit 方法的日志事件，内部保持一组输出
 */
public interface IEventProcessor extends ILifeCycle {

    @NotNull IMutableEvent event();

    void clearCrid();

    void setCrid(@Nullable String crid);

    @NotNull String getCrid();

    void clearPath();

    void setPath(@Nullable String path);

    @Nullable String getPath();

    @Nullable String getPathDigest();

}
