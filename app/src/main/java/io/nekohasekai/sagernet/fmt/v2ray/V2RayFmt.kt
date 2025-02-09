package io.nekohasekai.sagernet.fmt.v2ray

import android.text.TextUtils
import com.google.gson.Gson
import io.nekohasekai.sagernet.fmt.http.HttpBean
import io.nekohasekai.sagernet.fmt.trojan.TrojanBean
import io.nekohasekai.sagernet.ktx.*
import libcore.Libcore
import libcore.URL
import moe.matsuri.nb4a.SingBoxOptions.*
import moe.matsuri.nb4a.utils.listByLineOrComma

data class VmessQRCode(
    var v: String = "",
    var ps: String = "",
    var add: String = "",
    var port: String = "",
    var id: String = "",
    var aid: String = "0",
    var scy: String = "",
    var net: String = "",
    var packetEncoding: String = "",
    var type: String = "",
    var host: String = "",
    var path: String = "",
    var tls: String = "",
    var sni: String = "",
    var alpn: String = "",
    var fp: String = "",
)

fun StandardV2RayBean.isTLS(): Boolean {
    return security == "tls"
}

fun StandardV2RayBean.setTLS(boolean: Boolean) {
    security = if (boolean) "tls" else ""
}

fun parseV2Ray(rawUrl: String): StandardV2RayBean {
    // Try parse stupid formats first

    if (!rawUrl.contains("?")) {
        try {
            return parseV2RayN(rawUrl)
        } catch (e: Exception) {
            Logs.i("try v2rayN: " + e.readableMessage)
        }
    }

    // old V2Ray "std" format

    val bean = VMessBean().apply { if (rawUrl.startsWith("vless://")) alterId = -1 }
    val url = Libcore.parseURL(rawUrl)

    if (url.password.isNotBlank()) {
        // https://github.com/v2fly/v2fly-github-io/issues/26 (rarely use)
        bean.serverAddress = url.host
        bean.serverPort = url.ports.toIntOrNull()
        bean.name = url.fragment

        var protocol = url.username
        bean.v2rayTransport = protocol
        bean.alterId = url.password.substringAfterLast('-').toInt()
        bean.uuid = url.password.substringBeforeLast('-')

        if (protocol.endsWith("+tls")) {
            bean.security = "tls"
            protocol = protocol.substring(0, protocol.length - 4)

            url.queryParameterNotBlank("tlsServerName").let {
                if (it.isNotBlank()) {
                    bean.sni = it
                }
            }
        }

        when (protocol) {
//            "tcp" -> {
//                url.queryParameter("type")?.let { type ->
//                    if (type == "http") {
//                        bean.headerType = "http"
//                        url.queryParameter("host")?.let {
//                            bean.host = it
//                        }
//                    }
//                }
//            }
            "http" -> {
                url.queryParameterNotBlank("path").let {
                    bean.path = it
                }
                url.queryParameterNotBlank("host").let {
                    bean.host = it.split("|").joinToString(",")
                }
            }

            "ws" -> {
                url.queryParameterNotBlank("path").let {
                    bean.path = it
                }
                url.queryParameterNotBlank("host").let {
                    bean.host = it
                }
            }

            "grpc" -> {
                url.queryParameterNotBlank("serviceName").let {
                    bean.path = it
                }
            }

            "httpupgrade" -> {
                url.queryParameterNotBlank("path").let {
                    bean.path = it
                }
                url.queryParameterNotBlank("host").let {
                    bean.host = it
                }
            }
        }

        bean.packetEncoding = 1 // It comes from V2Ray!
    } else {
        // also vless format
        bean.parseDuckSoft(url)
    }

    return bean
}

// https://github.com/XTLS/Xray-core/issues/91
fun StandardV2RayBean.parseDuckSoft(url: URL) {
    serverAddress = url.host
    serverPort = url.ports.toIntOrNull() ?: 443
    name = url.fragment

    if (this is TrojanBean) {
        password = url.username
    } else {
        uuid = url.username
    }

    v2rayTransport = url.queryParameterNotBlank("type")
    if (v2rayTransport.isNullOrBlank()) v2rayTransport = "tcp"
    if (v2rayTransport == "h2") v2rayTransport = "http"

    security = url.queryParameterNotBlank("security")
    if (security.isNullOrBlank()) {
        security = if (this is TrojanBean) "tls" else "none"
    }

    when (security) {
        "tls", "reality" -> {
            security = "tls"
            url.queryParameterNotBlank("sni").let {
                sni = it
            }
            url.queryParameterNotBlank("host").let {
                if (sni.isNullOrBlank()) sni = it
            }
            url.queryParameterNotBlank("alpn").let {
                alpn = it
            }
            url.queryParameterNotBlank("cert").let {
                certificates = it
            }
            url.queryParameterNotBlank("pbk").let {
                realityPublicKey = it
            }
            url.queryParameterNotBlank("sid").let {
                realityShortID = it
            }
        }
    }

    when (v2rayTransport) {
        "tcp" -> {
            // v2rayNG
            if (url.queryParameterNotBlank("headerType") == "http") {
                url.queryParameterNotBlank("host").let {
                    v2rayTransport = "http"
                    host = it
                }
            }
        }

        "http" -> {
            url.queryParameterNotBlank("host").let {
                host = it
            }
            url.queryParameterNotBlank("path").let {
                path = it
            }
        }

        "ws" -> {
            url.queryParameterNotBlank("host").let {
                host = it
            }
            url.queryParameterNotBlank("path").let {
                path = it
            }
            url.queryParameterNotBlank("ed").let { ed ->
                if (ed.isNotBlank()) {
                    wsMaxEarlyData = ed.toIntOrNull() ?: 2048

                    url.queryParameterNotBlank("eh").let { eh ->
                        earlyDataHeaderName = eh.ifBlank {
                            "Sec-WebSocket-Protocol"
                        }
                    }
                }
            }
        }

        "grpc" -> {
            url.queryParameterNotBlank("serviceName").let {
                path = it
            }
        }

        "httpupgrade" -> {
            url.queryParameterNotBlank("host").let {
                host = it
            }
            url.queryParameterNotBlank("path").let {
                path = it
            }
        }
    }

    // maybe from Matsuri vmess export
    if (this is VMessBean && !isVLESS) {
        url.queryParameterNotBlank("encryption").let {
            encryption = it
        }
    }

    if (isVLESS) {
        url.queryParameterNotBlank("packetEncoding").let {
            when (it) {
                "packetaddr" -> packetEncoding = 1
                "xudp" -> packetEncoding = 2
            }
        }
    }

    url.queryParameterNotBlank("flow").let {
        if (isVLESS) {
            encryption = it.removeSuffix("-udp443")
        }
    }

    url.queryParameterNotBlank("fp").let {
        utlsFingerprint = it
    }
}

// SagerNet's
// Do not support some format and then throw exception
fun parseV2RayN(link: String): VMessBean {
    val result = link.substringAfter("vmess://").decodeBase64UrlSafe()
    if (result.contains("= vmess")) {
        return parseCsvVMess(result)
    }
    val bean = VMessBean()
    val vmessQRCode = Gson().fromJson(result, VmessQRCode::class.java)

    // Although VmessQRCode fields are non null, looks like Gson may still create null fields
    if (TextUtils.isEmpty(vmessQRCode.add)
        || TextUtils.isEmpty(vmessQRCode.port)
        || TextUtils.isEmpty(vmessQRCode.id)
        || TextUtils.isEmpty(vmessQRCode.net)
    ) {
        throw Exception("invalid VmessQRCode")
    }

    bean.name = vmessQRCode.ps
    bean.serverAddress = vmessQRCode.add
    bean.serverPort = vmessQRCode.port.toIntOrNull() ?: 10086
    bean.encryption = vmessQRCode.scy
    bean.uuid = vmessQRCode.id
    bean.alterId = vmessQRCode.aid.toIntOrNull() ?: 0
    bean.v2rayTransport = vmessQRCode.net
    bean.host = vmessQRCode.host
    bean.path = vmessQRCode.path
    val headerType = vmessQRCode.type

    when (vmessQRCode.packetEncoding) {
        "packetaddr" -> {
            bean.packetEncoding = 1
        }

        "xudp" -> {
            bean.packetEncoding = 2
        }
    }

    when (bean.v2rayTransport) {
        "tcp" -> {
            if (headerType == "http") {
                bean.v2rayTransport = "http"
            }
        }
    }
    when (vmessQRCode.tls) {
        "tls", "reality" -> {
            bean.security = "tls"
            bean.sni = vmessQRCode.sni
            if (bean.sni.isNullOrBlank()) bean.sni = bean.host
            bean.alpn = vmessQRCode.alpn
            bean.utlsFingerprint = vmessQRCode.fp
        }
    }

    return bean
}

private fun parseCsvVMess(csv: String): VMessBean {

    val args = csv.split(",")

    val bean = VMessBean()

    bean.serverAddress = args[1]
    bean.serverPort = args[2].toInt()
    bean.encryption = args[3]
    bean.uuid = args[4].replace("\"", "")

    args.subList(5, args.size).forEach {

        when {
            it == "over-tls=true" -> bean.security = "tls"
            it.startsWith("tls-host=") -> bean.host = it.substringAfter("=")
            it.startsWith("obfs=") -> bean.v2rayTransport = it.substringAfter("=")
            it.startsWith("obfs-path=") || it.contains("Host:") -> {
                runCatching {
                    bean.path = it.substringAfter("obfs-path=\"").substringBefore("\"obfs")
                }
                runCatching {
                    bean.host = it.substringAfter("Host:").substringBefore("[")
                }

            }

        }

    }

    return bean

}

fun StandardV2RayBean.toUriVMessVLESSTrojan(): String {

    var isTrojan = false
    val protocol = if (this is VMessBean) {
        if (isVLESS) "vless" else "vmess"
    } else {
        isTrojan = true
        "trojan"
    }

    // ducksoft fmt
    val builder = Libcore.newURL(protocol).apply {
        username = if (isTrojan) {
            (this@toUriVMessVLESSTrojan as TrojanBean).password
        } else {
            uuid
        }
        host = serverAddress
        ports = serverPort.toString()
        addQueryParameter("type", v2rayTransport)
    }

    if (!isTrojan) {
        if (isVLESS) {
            builder.addQueryParameter("flow", encryption)
        } else {
            builder.addQueryParameter("encryption", encryption)
        }
        when (packetEncoding) {
            1 -> {
                builder.addQueryParameter("packetEncoding", "packetaddr")
            }

            2 -> {
                builder.addQueryParameter("packetEncoding", "xudp")
            }
        }
    }

    when (v2rayTransport) {
        "tcp" -> {}
        "ws", "http", "httpupgrade" -> {
            if (host.isNotBlank()) {
                builder.addQueryParameter("host", host)
            }
            if (path.isNotBlank()) {
                builder.addQueryParameter("path", path)
            }
            if (v2rayTransport == "ws") {
                if (wsMaxEarlyData > 0) {
                    builder.addQueryParameter("ed", "$wsMaxEarlyData")
                    if (earlyDataHeaderName.isNotBlank()) {
                        builder.addQueryParameter("eh", earlyDataHeaderName)
                    }
                }
            } else if (v2rayTransport == "http" && !isTLS()) {
                builder.setQueryParameter("type", "tcp")
                builder.addQueryParameter("headerType", "http")
            }
        }

        "grpc" -> {
            if (path.isNotBlank()) {
                builder.setQueryParameter("serviceName", path)
            }
        }
    }

    if (security.isNotBlank() && security != "none") {
        builder.addQueryParameter("security", security)
        when (security) {
            "tls" -> {
                if (sni.isNotBlank()) {
                    builder.addQueryParameter("sni", sni)
                }
                if (alpn.isNotBlank()) {
                    builder.addQueryParameter("alpn", alpn.replace("\n", ","))
                }
                if (certificates.isNotBlank()) {
                    builder.addQueryParameter("cert", certificates)
                }
                if (allowInsecure) {
                    builder.addQueryParameter("allowInsecure", "1")
                }
                if (utlsFingerprint.isNotBlank()) {
                    builder.addQueryParameter("fp", utlsFingerprint)
                }
                if (realityPublicKey.isNotBlank()) {
                    builder.setQueryParameter("security", "reality")
                    builder.addQueryParameter("pbk", realityPublicKey)
                    builder.addQueryParameter("sid", realityShortID)
                }
            }
        }
    }

    if (name.isNotBlank()) {
        builder.fragment = name
    }

    return builder.string
}

fun buildSingBoxOutboundStreamSettings(bean: StandardV2RayBean): V2RayTransportOptions? {
    when (bean.v2rayTransport) {
        "tcp" -> {
            return null
        }

        "ws" -> {
            return V2RayTransportOptions_V2RayWebsocketOptions().apply {
                type = "ws"
                headers = mutableMapOf()

                if (bean.host.isNotBlank()) {
                    headers["Host"] = listOf(bean.host)
                }

                if (bean.path.contains("?ed=")) {
                    path = bean.path.substringBefore("?ed=")
                    max_early_data = bean.path.substringAfter("?ed=").toIntOrNull() ?: 2048
                    early_data_header_name = "Sec-WebSocket-Protocol"
                } else {
                    path = bean.path.takeIf { it.isNotBlank() } ?: "/"
                }

                if (bean.wsMaxEarlyData > 0) {
                    max_early_data = bean.wsMaxEarlyData
                }

                if (bean.earlyDataHeaderName.isNotBlank()) {
                    early_data_header_name = bean.earlyDataHeaderName
                }
            }
        }

        "http" -> {
            return V2RayTransportOptions_V2RayHTTPOptions().apply {
                type = "http"
                if (!bean.isTLS()) method = "GET" // v2ray tcp header
                if (bean.host.isNotBlank()) {
                    host = bean.host.split(",")
                }
                path = bean.path.takeIf { it.isNotBlank() } ?: "/"
            }
        }

        "quic" -> {
            return V2RayTransportOptions().apply {
                type = "quic"
            }
        }

        "grpc" -> {
            return V2RayTransportOptions_V2RayGRPCOptions().apply {
                type = "grpc"
                service_name = bean.path
            }
        }

        "httpupgrade" -> {
            return V2RayTransportOptions_V2RayHTTPUpgradeOptions().apply {
                type = "httpupgrade"
                host = bean.host
                path = bean.path
            }
        }
    }

//    if (needKeepAliveInterval) {
//        sockopt = StreamSettingsObject.SockoptObject().apply {
//            tcpKeepAliveInterval = keepAliveInterval
//        }
//    }

    return null
}

fun buildSingBoxOutboundTLS(bean: StandardV2RayBean): OutboundTLSOptions? {
    if (bean.security != "tls") return null
    return OutboundTLSOptions().apply {
        enabled = true
        insecure = bean.allowInsecure
        if (bean.sni.isNotBlank()) server_name = bean.sni
        if (bean.alpn.isNotBlank()) alpn = bean.alpn.listByLineOrComma()
        if (bean.certificates.isNotBlank()) certificate = listOf(bean.certificates)
        var fp = bean.utlsFingerprint
        if (bean.realityPublicKey.isNotBlank()) {
            reality = OutboundRealityOptions().apply {
                enabled = true
                public_key = bean.realityPublicKey
                short_id = bean.realityShortID
            }
            if (fp.isNullOrBlank()) fp = "chrome"
        }
        if (fp.isNotBlank()) {
            utls = OutboundUTLSOptions().apply {
                enabled = true
                fingerprint = fp
            }
        }
        if (bean.ech) {
            val echList = bean.echConfig.split("\n")
            ech = OutboundECHOptions().apply {
                enabled = true
                pq_signature_schemes_enabled = echList.size > 5
                dynamic_record_sizing_disabled = true
                config = echList

            }
        }
    }
}

fun buildSingBoxOutboundStandardV2RayBean(bean: StandardV2RayBean): Outbound {
    when (bean) {
        is HttpBean -> {
            return Outbound_HTTPOptions().apply {
                type = bean.outboundType()
                server = bean.serverAddress
                server_port = bean.serverPort
                username = bean.username
                password = bean.password
                tls = buildSingBoxOutboundTLS(bean)
            }
        }

        is VMessBean -> {
            if (bean.isVLESS) return Outbound_VLESSOptions().apply {
                type = bean.outboundType()
                server = bean.serverAddress
                server_port = bean.serverPort
                uuid = bean.uuid
                if (bean.encryption.isNotBlank() && bean.encryption != "auto") {
                    flow = bean.encryption
                }
                when (bean.packetEncoding) {
                    0 -> packet_encoding = ""
                    1 -> packet_encoding = "packetaddr"
                    2 -> packet_encoding = "xudp"
                }
                tls = buildSingBoxOutboundTLS(bean)
                transport = buildSingBoxOutboundStreamSettings(bean)
            }
            return Outbound_VMessOptions().apply {
                type = bean.outboundType()
                server = bean.serverAddress
                server_port = bean.serverPort
                uuid = bean.uuid
                alter_id = bean.alterId
                security = bean.encryption.takeIf { it.isNotBlank() } ?: "auto"
                when (bean.packetEncoding) {
                    0 -> packet_encoding = ""
                    1 -> packet_encoding = "packetaddr"
                    2 -> packet_encoding = "xudp"
                }
                tls = buildSingBoxOutboundTLS(bean)
                transport = buildSingBoxOutboundStreamSettings(bean)

                global_padding = true
                authenticated_length = bean.authenticatedLength
            }
        }

        is TrojanBean -> {
            return Outbound_TrojanOptions().apply {
                type = bean.outboundType()
                server = bean.serverAddress
                server_port = bean.serverPort
                password = bean.password
                tls = buildSingBoxOutboundTLS(bean)
                transport = buildSingBoxOutboundStreamSettings(bean)
            }
        }

        else -> throw IllegalStateException("can't reach")
    }
}
