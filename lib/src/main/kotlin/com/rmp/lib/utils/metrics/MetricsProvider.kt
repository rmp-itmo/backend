package com.rmp.lib.utils.metrics

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.log.Logger
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler

object MetricsProvider {
    fun start(prometheusMeterRegistry: PrometheusMeterRegistry) {
        if (!AppConf.metrics) {
            Logger.debug("Metrics exposing turned off")
            return
        }

        val server = Server(AppConf.metricsPort)

        server.handler = object : AbstractHandler() {
            override fun handle(
                target: String?,
                baseRequest: Request?,
                request: HttpServletRequest?,
                response: HttpServletResponse?
            ) {
                if (target == "/metrics-micrometer") {
                    response?.contentType = "text/plain"
                    response?.status = HttpServletResponse.SC_OK
                    response?.writer?.write(prometheusMeterRegistry.scrape())
                    baseRequest?.isHandled = true
                }
            }
        }

        server.start()
    }
}