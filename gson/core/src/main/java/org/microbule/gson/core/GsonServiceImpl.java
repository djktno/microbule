package org.microbule.gson.core;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.microbule.beanfinder.api.BeanFinder;
import org.microbule.beanfinder.api.BeanFinderListener;
import org.microbule.gson.api.GsonService;
import org.microbule.gson.spi.GsonCustomizer;

@Named
public class GsonServiceImpl implements GsonService {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private final List<GsonCustomizer> customizers = new CopyOnWriteArrayList<>();
    private final AtomicReference<Gson> gson = new AtomicReference<>(new GsonBuilder().create());

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    @Inject
    public GsonServiceImpl(BeanFinder beanFinder) {
        beanFinder.findBeans(GsonCustomizer.class, new GsonCustomizerListener());
    }

//----------------------------------------------------------------------------------------------------------------------
// GsonService Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public <T> T fromJson(Reader json, Type type) {
        return gson.get().fromJson(json, type);
    }

    @Override
    public void toJson(Object src, Type type, Appendable writer) {
        gson.get().toJson(src, type, writer);
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    private void addCustomizer(GsonCustomizer customizer) {
        customizers.add(customizer);
        rebuild();
    }

    private void rebuild() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        customizers.forEach(customizer -> customizer.customize(builder));
        gson.set(builder.create());
    }

    private void removeCustomizer(GsonCustomizer customizer) {
        if (customizers.remove(customizer)) {
            rebuild();
        }
    }

//----------------------------------------------------------------------------------------------------------------------
// Inner Classes
//----------------------------------------------------------------------------------------------------------------------

    private class GsonCustomizerListener implements BeanFinderListener<GsonCustomizer> {
//----------------------------------------------------------------------------------------------------------------------
// BeanFinderListener Implementation
//----------------------------------------------------------------------------------------------------------------------

        @Override
        public boolean beanFound(GsonCustomizer bean) {
            addCustomizer(bean);
            return true;
        }

        @Override
        public void beanLost(GsonCustomizer bean) {
            removeCustomizer(bean);
        }
    }
}