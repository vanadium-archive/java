// This file was auto-generated by the veyron vdl tool.
// Source: types.vdl

package com.veyron2.security;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * type ACL map[veyron2/security.PrincipalPattern string]veyron2/security.LabelSet uint32 
 * ACL pairs principal sets with label sets. This should be treated as an
 * unordered set, not as a dictionary.  A client with PublicID <pid> is
 * allowed access using <label> if there exists a pair (principals, labels)
 * in the ACL such that <pid>.Match(principals) and labels.HasLabel(label).
 **/
public final class ACL implements Map<com.veyron2.security.PrincipalPattern, com.veyron2.security.LabelSet> {
    private Map<com.veyron2.security.PrincipalPattern, com.veyron2.security.LabelSet> impl;

    public ACL(Map<com.veyron2.security.PrincipalPattern, com.veyron2.security.LabelSet> impl) {
        this.impl = impl;
    }

    public Map<com.veyron2.security.PrincipalPattern, com.veyron2.security.LabelSet> getValue() {
        return this.impl;
    }

    public void setValue(Map<com.veyron2.security.PrincipalPattern, com.veyron2.security.LabelSet> newImpl) {
        this.impl = newImpl;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;
        final ACL other = (ACL) obj;
        if (!(this.impl.equals(other.impl)))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return (impl == null) ? 0 : impl.hashCode();
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return impl.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return impl.containsValue(value);
    }

    @Override
    public Set<java.util.Map.Entry<com.veyron2.security.PrincipalPattern, com.veyron2.security.LabelSet>> entrySet() {
        return impl.entrySet();
    }

    @Override
    public com.veyron2.security.LabelSet get(Object key) {
        return impl.get(key);
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public Set<com.veyron2.security.PrincipalPattern> keySet() {
        return impl.keySet();
    }

    @Override
    public com.veyron2.security.LabelSet put(com.veyron2.security.PrincipalPattern key, com.veyron2.security.LabelSet value) {
        return impl.put(key, value);
    }

    @Override
    public void putAll(Map<? extends com.veyron2.security.PrincipalPattern, ? extends com.veyron2.security.LabelSet> map) {
        impl.putAll(map);
    }

    @Override
    public com.veyron2.security.LabelSet remove(Object key) {
        return impl.remove(key);
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public Collection<com.veyron2.security.LabelSet> values() {
        return impl.values();
    }
}
