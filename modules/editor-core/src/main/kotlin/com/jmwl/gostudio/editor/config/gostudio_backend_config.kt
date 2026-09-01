package com.jmwl.gostudio.editor.config

/**
 * GoStudio 远端服务配置。
 * 后续新增后端业务时，统一在这里补充 API 分区，避免把地址散落在各业务代码里。
 */
object gostudio_backend_config {
    const val API_BASE_URL = "https://gs.jmwl.dpdns.org/api"
    const val TRANSLATION_BASE_URL = "$API_BASE_URL/translation"

    // 后端开启了鉴权；该 key 只用于翻译接口限额，不是模型厂商 key。
    const val TRANSLATION_BACKEND_KEY = "bbad25bca266d179d8b93e16e888da6d846575c1bd37032e"
}
