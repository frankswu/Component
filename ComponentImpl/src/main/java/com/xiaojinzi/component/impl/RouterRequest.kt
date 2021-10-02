package com.xiaojinzi.component.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.xiaojinzi.component.Component.requiredConfig
import com.xiaojinzi.component.ComponentActivityStack.getTopAliveActivity
import com.xiaojinzi.component.anno.support.CheckClassNameAnno
import com.xiaojinzi.component.support.*
import java.util.*

/**
 * 表示路由的一个请求类,构建时候如果参数不对是有异常会发生的,使用的时候注意这一点
 * 但是在拦截器 [RouterInterceptor] 中构建是不用关心错误的,
 * 因为拦截器的 [RouterInterceptor.intercept] 方法
 * 允许抛出异常
 *
 *
 * time   : 2018/11/29
 *
 * @author xiaojinzi
 */
@CheckClassNameAnno
class RouterRequest private constructor(builder: RouterRequestBuilderImpl<*>) {

    companion object {
        const val KEY_SYNC_URI = "_componentSyncUri"
    }

    @JvmField
    val context: Context? = builder.context

    @JvmField
    val fragment: Fragment? = builder.fragment

    /**
     * 这是一个很重要的参数, 一定不可以为空,如果这个为空了,一定不能继续下去,因为很多地方直接使用这个参数的,不做空判断的
     * 而且这个参数不可以为空的
     */
    @JvmField
    val uri: Uri = builder.buildURI()

    /**
     * requestCode
     */
    @JvmField
    val requestCode: Int? = builder.requestCode

    /**
     * 框架是否帮助用户跳转拿 [com.xiaojinzi.component.bean.ActivityResult]
     * 有 requestCode 只能说明用户使用了某一个 requestCode,
     * 会调用 [Activity.startActivityForResult].
     * 但是不代表需要框架去帮你获取到 [com.xiaojinzi.component.bean.ActivityResult].
     * 所以这个值就是标记是否需要框架帮助您去获取 [com.xiaojinzi.component.bean.ActivityResult]
     */
    @JvmField
    val isForResult: Boolean = builder.isForResult

    /**
     * 是否是为了目标 Intent 来的
     */
    @JvmField
    val isForTargetIntent: Boolean = builder.isForTargetIntent

    /**
     * 跳转的时候 options 参数
     */
    @JvmField
    val options: Bundle? = builder.options

    /**
     * Intent 的 flag, 集合不可更改
     */
    @JvmField
    val intentFlags: List<Int> = Collections.unmodifiableList(builder.intentFlags)

    /**
     * Intent 的 类别, 集合不可更改
     */
    @JvmField
    val intentCategories: List<String> = Collections.unmodifiableList(builder.intentCategories)

    @JvmField
    val bundle: Bundle = Bundle().apply {
        this.putAll(builder.bundle)
    }

    @JvmField
    val intentConsumer: Consumer<Intent>? = builder.intentConsumer

    /**
     * 这个 [Action] 是在路由开始的时候调用的.
     * 和 [Activity.startActivity] 不是连着执行的.
     * 中间 post 到主线程的操作
     */
    @JvmField
    val beforeAction: (() -> Unit)? = builder.beforeAction

    /**
     * 这个 [Action] 是在 [Activity.startActivity] 之前调用的.
     * 和 [Activity.startActivity] 是连着执行的.
     */
    @JvmField
    val beforeStartAction: (() -> Unit)? = builder.beforeStartAction

    /**
     * 这个 [Action] 是在 [Activity.startActivity] 之后调用的.
     * 和 [Activity.startActivity] 是连着执行的.
     */
    @JvmField
    val afterStartAction: (() -> Unit)? = builder.afterStartAction

    /**
     * 这个 [Action] 是在结束之后调用的.
     * 和 [Activity.startActivity] 不是连着执行的.
     * 是在 [RouterInterceptor.Callback.onSuccess]
     * 方法中 post 到主线程完成的
     */
    @JvmField
    val afterAction: (() -> Unit)? = builder.afterAction

    /**
     * 这个 [Action] 是在结束之后调用的.
     * 和 [Activity.startActivity] 不是连着执行的.
     * 是在 [RouterInterceptor.Callback.onError]
     * 方法中 post 到主线程完成的
     */
    @JvmField
    val afterErrorAction: (() -> Unit)? = builder.afterErrorAction

    /**
     * 这个 [Action] 是在结束之后调用的.
     * 和 [Activity.startActivity] 不是连着执行的.
     * 是在 [RouterInterceptor.Callback.onSuccess] 或者
     * [RouterInterceptor.Callback.onError]
     * 方法中 post 到主线程完成的
     */
    @JvmField
    val afterEventAction: (() -> Unit)? = builder.afterEventAction

    /**
     * 同步 Query 到 Bundle 中
     */
    fun syncUriToBundle() {
        // 如果 URI 没有变化就不同步了
        if (bundle.getInt(KEY_SYNC_URI) == uri.hashCode()) {
            return
        }
        ParameterSupport.syncUriToBundle(uri, bundle)
        // 更新新的 hashCode
        bundle.putInt(KEY_SYNC_URI, uri.hashCode())
    }// 如果是 Activity 并且已经销毁了返回 null// 如果不是 Activity 可能是 Application,所以直接返回

    /**
     * 从 [Fragment] 和 [Context] 中获取上下文
     *
     *
     * 参数中的 [RouterRequest.context] 可能是一个 [android.app.Application] 或者是一个
     * [android.content.ContextWrapper] 或者是一个 [Activity]
     * 无论参数的类型是哪种, 此方法的返回值就只有两种类型：
     * 1. [android.app.Application]
     * 2. [Activity]
     *
     *
     * 如果返回的是 [Activity] 的 [Context],
     * 当 [Activity] 销毁了就会返回 null
     * 另外就是返回 [android.app.Application]
     *
     * @return [Context], 可能为 null, null 就只有一种情况就是界面销毁了.
     * 构建 [RouterRequest] 的时候已经保证了
     */
    val rawContext: Context?
        get() {
            var rawContext: Context? = null
            if (context != null) {
                rawContext = context
            } else if (fragment != null) {
                rawContext = fragment.context
            }
            val rawAct = Utils.getActivityFromContext(rawContext)
            // 如果不是 Activity 可能是 Application,所以直接返回
            return if (rawAct == null) {
                rawContext
            } else {
                // 如果是 Activity 并且已经销毁了返回 null
                if (Utils.isActivityDestoryed(rawAct)) {
                    null
                } else {
                    rawContext
                }
            }
        }

    /**
     * 从 [Context] 中获取 [Activity], [Context] 可能是 [android.content.ContextWrapper]
     *
     * @return 如果 Activity 销毁了就会返回 null
     */
    val activity: Activity?
        get() {
            if (context == null) {
                return null
            }
            val realActivity = Utils.getActivityFromContext(context)
                    ?: return null
            return if (Utils.isActivityDestoryed(realActivity)) {
                null
            } else realActivity
        }

    /**
     * 从参数 [Fragment] 和 [Context] 获取 Activity,
     *
     * @return 如果 activity 已经销毁并且 fragment 销毁了就会返回 null
     */
    val rawActivity: Activity?
        get() {
            var rawActivity = activity
            if (rawActivity == null) {
                if (fragment != null) {
                    rawActivity = fragment.activity
                }
            }
            if (rawActivity == null) {
                return null
            }
            return if (Utils.isActivityDestoryed(rawActivity)) {
                null
            } else {
                // 如果不是为空返回的, 那么必定不是销毁的
                rawActivity
            }
        }

    /**
     * 首先调用 [.getRawActivity] 尝试获取此次用户传入的 Context 中是否有关联的 Activity
     * 如果为空, 则尝试获取运行中的所有 Activity 中顶层的那个
     */
    val rawOrTopActivity: Activity?
        get() {
            var result = rawActivity
            if (result == null) {
                // 如果不是为空返回的, 那么必定不是销毁的
                result = getTopAliveActivity()
            }
            return result
        }

    /**
     * 这里转化的对象会比 [RouterRequestBuilder] 对象中的参数少一个 [RouterRequestBuilder.url]
     * 因为 uri 转化为了 scheme,host,path,queryMap 那么这时候就不需要 url 了
     */
    fun toBuilder(): RouterRequestBuilder {
        val builder = RouterRequestBuilder()

        // 有关界面的两个
        builder.fragment = fragment
        builder.context = context

        // 还原一个 Uri 为各个零散的参数
        builder.scheme(scheme = uri.scheme!!)
        builder.host(host = uri.host!!)
        builder.path(path = uri.path!!)
        val queryParameterNames = uri.queryParameterNames
        if (queryParameterNames != null) {
            for (queryParameterName in queryParameterNames) {
                builder.query(queryName = queryParameterName, queryValue = uri.getQueryParameter(queryParameterName)!!)
            }
        }
        builder.bundle.putAll(bundle)
        builder.requestCode = requestCode
        builder.isForResult = isForResult
        builder.isForTargetIntent = isForTargetIntent
        builder.options(options = options)
        // 这里需要新创建一个是因为不可修改的集合不可以给别人
        builder.addIntentCategories(*intentCategories.toTypedArray())
        builder.addIntentFlags(*intentFlags.toIntArray())
        builder.intentConsumer(intentConsumer = intentConsumer)
        builder.beforeAction(action = beforeAction)
        builder.beforeStartAction(action = beforeStartAction)
        builder.afterStartAction(action = afterStartAction)
        builder.afterAction(action = afterAction)
        builder.afterErrorAction(action = afterErrorAction)
        builder.afterEventAction(action = afterEventAction)
        return builder
    }

    interface IRouterRequestBuilder<T : IRouterRequestBuilder<T>> :
            IURIBuilder<T>,
            IBundleBuilder<T> {

        /**
         * 是否是跳转拿 [com.xiaojinzi.component.bean.ActivityResult] 的
         */
        var isForResult: Boolean

        /**
         * 是否是为了目标 Intent
         */
        var isForTargetIntent: Boolean

        var context: Context?
        var fragment: Fragment?
        var requestCode: Int?

        fun requestCode(requestCode: Int?): T
        fun beforeAction(@UiThread action: Action?): T
        fun beforeAction(@UiThread action: (() -> Unit)?): T
        fun beforeStartAction(@UiThread action: Action?): T
        fun beforeStartAction(@UiThread action: (() -> Unit)?): T
        fun afterStartAction(@UiThread action: Action?): T
        fun afterStartAction(@UiThread action: (() -> Unit)?): T
        fun afterAction(@UiThread action: Action?): T
        fun afterAction(@UiThread action: (() -> Unit)?): T
        fun afterErrorAction(@UiThread action: Action?): T
        fun afterErrorAction(@UiThread action: (() -> Unit)?): T
        fun afterEventAction(@UiThread action: Action?): T
        fun afterEventAction(@UiThread action: (() -> Unit)?): T
        fun intentConsumer(@UiThread intentConsumer: Consumer<Intent>?): T
        fun addIntentFlags(vararg flags: Int): T
        fun addIntentCategories(vararg categories: String): T
        fun options(options: Bundle?): T
        fun build(): RouterRequest

    }

    open class RouterRequestBuilderImpl<T : IRouterRequestBuilder<T>>(
            context: Context? = null,
            fragment: Fragment? = null,
            private val uriBuilder: IURIBuilder<T> = IURIBuilderImpl(),
            private val bundleBuilder: IBundleBuilder<T> = IBundleBuilderImpl(),
            private val targetDelegateImplCallable: DelegateImplCallable<T> = DelegateImplCallableImpl()
    ) : IRouterRequestBuilder<T>,
            IURIBuilder<T> by uriBuilder,
            IBundleBuilder<T> by bundleBuilder,
            DelegateImplCallable<T> by targetDelegateImplCallable {

        var options: Bundle? = null

        /**
         * Intent 的 flag,允许修改的
         */
        var intentFlags: MutableList<Int> = ArrayList(2)

        /**
         * Intent 的 类别,允许修改的
         */
        var intentCategories: MutableList<String> = ArrayList(2)

        override var context: Context? = null
        override var fragment: Fragment? = null
        override var requestCode: Int? = null
        override var isForResult = false
        override var isForTargetIntent = false

        var intentConsumer: Consumer<Intent>? = null

        /**
         * 路由开始之前
         */
        var beforeAction: (() -> Unit)? = null

        /**
         * 执行 [Activity.startActivity] 之前
         */
        var beforeStartAction: (() -> Unit)? = null

        /**
         * 执行 [Activity.startActivity] 之后
         */
        var afterStartAction: (() -> Unit)? = null

        /**
         * 跳转成功之后的 Callback
         * 此时的跳转成功仅代表目标界面启动成功, 不代表跳转拿数据的回调被回调了
         * 假如你是跳转拿数据的, 当你跳转到 A 界面, 此回调就会回调了,
         * 当你拿到 Intent 的回调了, 和此回调已经没关系了
         */
        var afterAction: (() -> Unit)? = null

        /**
         * 跳转失败之后的 Callback
         */
        var afterErrorAction: (() -> Unit)? = null

        /**
         * 跳转成功和失败之后的 Callback
         */
        var afterEventAction: (() -> Unit)? = null

        override var delegateImplCallable: () -> T
            get() = targetDelegateImplCallable.delegateImplCallable
            set(value) {
                uriBuilder.delegateImplCallable = value
                bundleBuilder.delegateImplCallable = value
                targetDelegateImplCallable.delegateImplCallable = value
            }

        private fun getRealDelegateImpl(): T {
            return delegateImplCallable.invoke()
        }

        override fun requestCode(requestCode: Int?): T {
            this.requestCode = requestCode
            return getRealDelegateImpl()
        }

        override fun beforeAction(@UiThread action: Action?): T {
            return beforeAction(action = {
                action?.run()
            })
        }

        override fun beforeAction(@UiThread action: (() -> Unit)?): T {
            beforeAction = action
            return getRealDelegateImpl()
        }

        override fun beforeStartAction(@UiThread action: Action?): T {
            return beforeStartAction(action = {
                action?.run()
            })
        }

        override fun beforeStartAction(@UiThread action: (() -> Unit)?): T {
            beforeStartAction = action
            return getRealDelegateImpl()
        }

        override fun afterStartAction(@UiThread action: Action?): T {
            return afterStartAction(action = {
                action?.run()
            })
        }

        override fun afterStartAction(@UiThread action: (() -> Unit)?): T {
            afterStartAction = action
            return getRealDelegateImpl()
        }

        override fun afterAction(@UiThread action: Action?): T {
            return afterAction(action = {
                action?.run()
            })
        }

        override fun afterAction(@UiThread action: (() -> Unit)?): T {
            afterAction = action
            return getRealDelegateImpl()
        }

        override fun afterErrorAction(@UiThread action: Action?): T {
            return afterErrorAction(action = {
                action?.run()
            })
        }

        override fun afterErrorAction(@UiThread action: (() -> Unit)?): T {
            afterErrorAction = action
            return getRealDelegateImpl()
        }

        override fun afterEventAction(@UiThread action: Action?): T {
            return afterEventAction(action = {
                action?.run()
            })
        }

        override fun afterEventAction(@UiThread action: (() -> Unit)?): T {
            afterEventAction = action
            return getRealDelegateImpl()
        }

        /**
         * 当不是自定义跳转的时候, Intent 由框架生成,所以可以回调这个接口
         * 当自定义跳转,这个回调不会回调的,这是需要注意的点
         *
         *
         * 其实目标界面可以完全的自定义路由,这个功能实际上没有存在的必要,因为你可以为同一个界面添加上多个 [com.xiaojinzi.component.anno.RouterAnno]
         * 然后每一个 [com.xiaojinzi.component.anno.RouterAnno] 都可以有不同的行为.是可以完全的代替 [RouterRequest.intentConsumer] 方法的
         *
         * @param intentConsumer Intent 是框架自动构建完成的,里面有跳转需要的所有参数和数据,这里就是给用户一个
         * 更改的机会,最好别更改内部的参数等的信息,这里提供出来其实主要是可以让你调用Intent
         * 的 [Intent.addFlags] 等方法,并不是给你修改内部的 bundle 的
         */
        override fun intentConsumer(@UiThread intentConsumer: Consumer<Intent>?): T {
            this.intentConsumer = intentConsumer
            return getRealDelegateImpl()
        }

        override fun addIntentFlags(vararg flags: Int): T {
            intentFlags.addAll(flags.toList())
            return getRealDelegateImpl()
        }

        override fun addIntentCategories(vararg categories: String): T {
            intentCategories.addAll(listOf(*categories))
            return getRealDelegateImpl()
        }

        /**
         * 用于 API >= 16 的时候,调用 [Activity.startActivity]
         */
        override fun options(options: Bundle?): T {
            this.options = options
            return getRealDelegateImpl()
        }

        /**
         * 构建请求对象,这个构建是必须的,不能错误的,如果出错了,直接崩溃掉,因为连最基本的信息都不全没法进行下一步的操作
         *
         * @return 可能会抛出一个运行时异常, 由于您的参数在构建 uri 的时候出现的异常
         */
        override fun build(): RouterRequest {
            return RouterRequest(this)
        }

        init {
            this.context = context
            this.fragment = fragment
        }

    }

    /**
     * 构建一个路由请求对象 [RouterRequest] 对象的 Builder
     *
     * @author xiaojinzi
     */
    class RouterRequestBuilder(
            private val routerRequestBuilder: IRouterRequestBuilder<RouterRequestBuilder> = RouterRequestBuilderImpl(),
    ) : IRouterRequestBuilder<RouterRequestBuilder> by routerRequestBuilder {

        init {
            routerRequestBuilder.delegateImplCallable = {
                this
            }
        }

    }

    interface IURIBuilder<T : IURIBuilder<T>> : DelegateImplCallable<T> {

        fun url(url: String): T
        fun scheme(scheme: String): T
        fun hostAndPath(hostAndPath: String): T
        fun userInfo(userInfo: String): T
        fun host(host: String): T
        fun path(path: String): T
        fun query(queryName: String, queryValue: String): T
        fun query(queryName: String, queryValue: Boolean): T
        fun query(queryName: String, queryValue: Byte): T
        fun query(queryName: String, queryValue: Int): T
        fun query(queryName: String, queryValue: Float): T
        fun query(queryName: String, queryValue: Long): T
        fun query(queryName: String, queryValue: Double): T

        /**
         * 构建一个 [Uri],如果构建失败会抛出异常
         */
        fun buildURI(): Uri

        /**
         * 构建一个URL,如果构建失败会抛出异常
         */
        fun buildURL(): String {
            return buildURI().toString()
        }

    }

    /**
     * 构造 URI 和 URL 的Builder
     *
     * @author xiaojinzi
     */
    open class IURIBuilderImpl<T : IURIBuilder<T>>(
            private val targetDelegateImplCallable: DelegateImplCallable<T> = DelegateImplCallableImpl(),
    ) : IURIBuilder<T>, DelegateImplCallable<T> by targetDelegateImplCallable
    // 占位
    {

        var url: String? = null
        var scheme: String? = null
        private var userInfo: String? = null
        var host: String? = null
        var path: String? = null
        private var queryMap: MutableMap<String, String> = HashMap()

        override var delegateImplCallable: () -> T
            get() = targetDelegateImplCallable.delegateImplCallable
            set(value) {
                targetDelegateImplCallable.delegateImplCallable = value
            }

        private fun getRealDelegateImpl(): T {
            return delegateImplCallable.invoke()
        }

        override fun url(url: String): T {
            this.url = url
            return getRealDelegateImpl()
        }

        override fun scheme(scheme: String): T {
            this.scheme = scheme
            return getRealDelegateImpl()
        }

        /**
         * xxx/xxx
         *
         * @param hostAndPath xxx/xxx
         */
        override fun hostAndPath(hostAndPath: String): T {
            Utils.checkNullPointer(hostAndPath, "hostAndPath")
            val index = hostAndPath.indexOf("/")
            if (index > 0) {
                host(hostAndPath.substring(0, index))
                path(hostAndPath.substring(index + 1))
            } else {
                Utils.debugThrowException(IllegalArgumentException("$hostAndPath is invalid"))
            }
            return getRealDelegateImpl()
        }

        override fun userInfo(userInfo: String): T {
            Utils.checkStringNullPointer(userInfo, "userInfo")
            this.userInfo = userInfo
            return getRealDelegateImpl()
        }

        override fun host(host: String): T {
            Utils.checkStringNullPointer(host, "host")
            this.host = host
            return getRealDelegateImpl()
        }

        override fun path(path: String): T {
            Utils.checkStringNullPointer(path, "path")
            this.path = path
            return getRealDelegateImpl()
        }

        override fun query(queryName: String, queryValue: String): T {
            Utils.checkStringNullPointer(queryName, "queryName")
            Utils.checkStringNullPointer(queryValue, "queryValue")
            queryMap[queryName] = queryValue
            return getRealDelegateImpl()
        }

        override fun query(queryName: String, queryValue: Boolean): T {
            return query(queryName, queryValue.toString())
        }

        override fun query(queryName: String, queryValue: Byte): T {
            return query(queryName, queryValue.toString())
        }

        override fun query(queryName: String, queryValue: Int): T {
            return query(queryName, queryValue.toString())
        }

        override fun query(queryName: String, queryValue: Float): T {
            return query(queryName, queryValue.toString())
        }

        override fun query(queryName: String, queryValue: Long): T {
            return query(queryName, queryValue.toString())
        }

        override fun query(queryName: String, queryValue: Double): T {
            return query(queryName, queryValue.toString())
        }

        override fun buildURI(): Uri {
            val builder = this
            var result: Uri?
            if (builder.url == null) {
                val uriBuilder = Uri.Builder()
                val authoritySB = StringBuffer()
                if (!userInfo.isNullOrEmpty()) {
                    authoritySB
                            .append(Uri.encode(userInfo))
                            .append("@")
                }
                authoritySB.append(
                        Uri.encode(
                                Utils.checkStringNullPointer(
                                        builder.host, "host",
                                        "do you forget call host() to set host?"
                                )
                        )
                )
                uriBuilder
                        .scheme(if (TextUtils.isEmpty(builder.scheme)) requiredConfig().defaultScheme else builder.scheme) // host 一定不能为空
                        .encodedAuthority(authoritySB.toString())
                        .path(
                                Utils.checkStringNullPointer(
                                        builder.path, "path",
                                        "do you forget call path() to set path?"
                                )
                        )
                for ((key, value) in builder.queryMap) {
                    uriBuilder.appendQueryParameter(key, value)
                }
                result = uriBuilder.build()
            } else {
                result = Uri.parse(builder.url)
                if (builder.queryMap.isNotEmpty()) {
                    val uriBuilder = result.buildUpon()
                    for ((key, value) in builder.queryMap) {
                        uriBuilder.appendQueryParameter(key, value)
                    }
                    result = uriBuilder.build()
                }
            }
            return result
        }

    }

    class URIBuilder(
            private val uriBuilder: IURIBuilder<URIBuilder> = IURIBuilderImpl(),
    ) : IURIBuilder<URIBuilder> by uriBuilder {

        override var delegateImplCallable: () -> URIBuilder
            get() = uriBuilder.delegateImplCallable
            set(value) {
                uriBuilder.delegateImplCallable = value
            }

        init {
            delegateImplCallable = {
                this
            }
        }

    }

}