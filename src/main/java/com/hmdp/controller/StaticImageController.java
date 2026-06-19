package com.hmdp.controller;

import cn.hutool.core.util.StrUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class StaticImageController {

    @GetMapping(value = "/imgs/{type}/{filename:.+}", produces = "image/svg+xml;charset=UTF-8")
    public byte[] imagePlaceholder(@PathVariable String type, @PathVariable String filename) {
        return createPlaceholder("演示图片", resolveColor(type), filename).getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/types/{filename:.+}", produces = "image/svg+xml;charset=UTF-8")
    public byte[] typePlaceholder(@PathVariable String filename) {
        return createPlaceholder("分类图标", "#42b883", filename).getBytes(StandardCharsets.UTF_8);
    }

    private String resolveColor(String type) {
        if ("blogs".equals(type)) {
            return "#ff8a65";
        }
        if ("icons".equals(type)) {
            return "#7e57c2";
        }
        return "#26a69a";
    }

    private String createPlaceholder(String title, String color, String filename) {
        String safeTitle = escapeXml(title);
        String safeFilename = escapeXml(StrUtil.blankToDefault(filename, "hmdp"));
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"480\" viewBox=\"0 0 800 480\">"
                + "<defs><linearGradient id=\"g\" x1=\"0\" x2=\"1\" y1=\"0\" y2=\"1\">"
                + "<stop offset=\"0%\" stop-color=\"" + color + "\"/>"
                + "<stop offset=\"100%\" stop-color=\"#eef6f6\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"800\" height=\"480\" rx=\"32\" fill=\"url(#g)\"/>"
                + "<circle cx=\"650\" cy=\"90\" r=\"90\" fill=\"rgba(255,255,255,.28)\"/>"
                + "<circle cx=\"120\" cy=\"390\" r=\"120\" fill=\"rgba(255,255,255,.2)\"/>"
                + "<text x=\"50%\" y=\"46%\" dominant-baseline=\"middle\" text-anchor=\"middle\" fill=\"#ffffff\" font-size=\"48\" font-family=\"Arial, sans-serif\" font-weight=\"700\">"
                + safeTitle
                + "</text>"
                + "<text x=\"50%\" y=\"58%\" dominant-baseline=\"middle\" text-anchor=\"middle\" fill=\"rgba(255,255,255,.86)\" font-size=\"22\" font-family=\"Arial, sans-serif\">"
                + safeFilename
                + "</text>"
                + "</svg>";
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
