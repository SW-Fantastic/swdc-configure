package org.swdc.language;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 语言和本地化处理。
 */
public class Locals {

    private Path configPath;
    private Map<String, JsonNode> loadedLanguages = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * 加载所有的语言文件
     * @param configPath 配置文件，json格式，是一个json的Object，
     *                   内容是语言名称和其配置文件名称或者路径的对应关系。
     */
    public Locals(Path configPath) {
        this.configPath = configPath;
        try {
            InputStream in = Files.newInputStream(configPath);
            Path parent = configPath.toAbsolutePath().getParent();
            Map<String,String> languagesNode = mapper.readValue(in,Map.class);
            for (Map.Entry<String,String> ent: languagesNode.entrySet()) {
                String lang = ent.getKey();
                Path target = parent.resolve(ent.getValue());
                InputStream langIn = Files.newInputStream(target);
                JsonNode node = mapper.readTree(langIn);
                this.loadedLanguages.put(lang,node);

                langIn.close();
            }
            in.close();
        } catch (Exception e){
            throw new RuntimeException("无法读取国际化（语言）文件。",e);
        }
    }

    /**
     * 加载指定的语言文件
     * @param configPath 配置文件，json格式，是一个json的Object，
     *                   内容是语言名称和其配置文件名称或者路径的对应关系。
     *
     * @param local 指定要加载的语言名称。
     */
    public Locals(Path configPath, String local) {
        this.configPath = configPath;
        try {
            InputStream in = Files.newInputStream(configPath);
            Path parent = configPath.toAbsolutePath().getParent();
            Map<String, String> languagesNode = mapper.readValue(in, Map.class);
            if (!languagesNode.containsKey(local)) {
                throw new RuntimeException("找不到语言：" + local);
            }
            InputStream lang = Files.newInputStream(parent.resolve(languagesNode.get(local)));
            JsonNode langNode = mapper.readTree(lang);

            loadedLanguages.put(local,langNode);

            in.close();
            lang.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取特定的一种language
     * @param name
     * @return
     */
    public Language getLanguage(String name) {
        return new DefaultLanguage(loadedLanguages.get(name));
    }


}
