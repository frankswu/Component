package com.xiaojinzi.component.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.xiaojinzi.component.anno.support.CheckClassNameAnno;
import com.xiaojinzi.component.support.Function;
import com.xiaojinzi.component.support.OnRouterCancel;
import com.xiaojinzi.component.support.OnRouterError;
import com.xiaojinzi.component.support.Utils;

/**
 * {@link Callback} 在这个基础上, 表示可以携带一个参数的回调
 * 当整个流程完成的时候,回调这个接口
 * <p>
 * 详细的请查看 {@link Callback}
 *
 * @author xiaojinzi
 */
@CheckClassNameAnno
public interface BiCallback_backup<T> extends OnRouterCancel, OnRouterError {

    /**
     * 当路由成功的时候,回调
     *
     * @param result 路由成功的对象
     * @param t      返回的对象
     */
    @UiThread
    void onSuccess(@NonNull RouterResult result, @NonNull T t);

    /**
     * 做一个转化
     *
     * @param <T> T 转化为 R
     * @param <R> T 转化为 R
     */
    abstract class Map<T, R> implements BiCallback_backup<T>, Function<T, R> {

        @NonNull
        private BiCallback_backup targetBiCallback;

        public Map(@NonNull BiCallback_backup targetBiCallback) {
            Utils.checkNullPointer(targetBiCallback, "targetBiCallback");
            this.targetBiCallback = targetBiCallback;
        }

        @Override
        public void onSuccess(@NonNull RouterResult result, @NonNull T t) {
            try {
                targetBiCallback.onSuccess(result, Utils.checkNullPointer(apply(t), "apply(t)"));
            } catch (Exception e) {
                targetBiCallback.onError(new RouterErrorResult(e));
            }
        }

        @Override
        public void onCancel(@Nullable RouterRequest originalRequest) {
            targetBiCallback.onCancel(originalRequest);
        }

        @Override
        public void onError(@NonNull RouterErrorResult errorResult) {
            targetBiCallback.onError(errorResult);
        }

    }

    /**
     * 空白实现类
     */
    class BiCallbackAdapter<T> implements BiCallback_backup<T> {

        @Override
        public void onSuccess(@NonNull RouterResult result, @NonNull T t) {
        }

        @Override
        public void onError(@NonNull RouterErrorResult errorResult) {
        }

        @Override
        public void onCancel(@NonNull RouterRequest originalRequest) {
        }

    }

}
