package com.formatic.service;

import com.formatic.form.FormFieldMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class MetadataCacheService {

    private final Map<Class<?>, List<FormFieldMetadata>> cache = new ConcurrentHashMap<>();

    public List<FormFieldMetadata> getMetadataForClass(Class<?> clazz) {
        return cache.get(clazz);
    }

    public void putMetadataForClass(Class<?> clazz, List<FormFieldMetadata> metadata) {
        cache.put(clazz, metadata);
    }

    public boolean containsClass(Class<?> clazz) {
        return cache.containsKey(clazz);
    }

    public void clearCache() {
        cache.clear();
      //  log.info("Cache des métadonnées vidé");
    }

    public void clearCacheForClass(Class<?> clazz) {
        cache.remove(clazz);
       // log.info("Cache vidé pour la classe: {}", clazz.getSimpleName());
    }

    public int getCacheSize() {
        return cache.size();
    }


}

