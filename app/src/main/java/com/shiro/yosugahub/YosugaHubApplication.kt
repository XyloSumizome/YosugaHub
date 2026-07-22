package com.shiro.yosugahub

import android.app.Application
import com.shiro.yosugahub.di.AppContainer
import com.shiro.yosugahub.di.DefaultAppContainer

/** アプリ全体で共有する DI コンテナを保持する Application。 */
class YosugaHubApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
