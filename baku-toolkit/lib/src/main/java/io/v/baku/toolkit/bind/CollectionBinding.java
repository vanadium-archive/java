// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import io.v.v23.syncbase.nosql.PrefixRange;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionBinding {
    /**
     * Builder class for collection Syncbase bindings, which are read-only bindings from Syncbase
     * data to collections of UI elements, such as items in a ListView or RecyclerView. In addition
     * to defining the Syncbase data being mapped, collection bindings are responsible for
     * ordering/arranging the data and mapping them to their view elements (they are Android widget
     * adapters). Writes are generally done through direct database writes.
     *
     * Two forms of collection binding are currently available:
     *
     * * {@linkplain PrefixBindingBuilder Prefix bindings}, built via the {@link #onPrefix(String)}
     *   or {@link #onPrefix(PrefixRange)} method
     * * {@linkplain IdListBindingBuilder ID-list bindings}, built via the {@link #onIdList(String)}
     *   method
     */
    public static class Builder extends BaseCollectionBindingBuilder<Builder> {
        /**
         * Invokes a {@link PrefixBindingBuilder} on the given row name prefix.
         */
        public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> onPrefix(
                final String prefix) {
            return new PrefixBindingBuilder<T, A>(this).prefix(prefix);
        }

        /**
         * Invokes a {@link PrefixBindingBuilder} on the given row name prefix.
         */
        public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> onPrefix(
                final PrefixRange prefix) {
            return new PrefixBindingBuilder<T, A>(this).prefix(prefix);
        }

        /**
         * Invokes a {@link IdListBindingBuilder} on the given ID list row name.
         */
        public <A extends RangeAdapter> IdListBindingBuilder<A> onIdList(
                final String idListRowName) {
            return new IdListBindingBuilder<A>(this).idListRowName(idListRowName);
        }
    }

    /**
     * @see io.v.baku.toolkit.bind.CollectionBinding.Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
