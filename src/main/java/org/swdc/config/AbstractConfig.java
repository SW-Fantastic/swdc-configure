package org.swdc.config;

import org.swdc.config.converters.Converters;


public abstract class AbstractConfig implements Configure {

    private Configure parent;
    private Converters converters = new Converters();

    protected abstract void saveInternal();

    public Configure getParent() {
        return parent;
    }

    public Converters getConverters() {
        return converters;
    }

    protected void setParent(Configure parent) {
        this.parent = parent;
    }

    @Override
    public void save() {
        if (parent != null) {
            parent.save();
        } else {
            this.saveInternal();
        }
    }

}
