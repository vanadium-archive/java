// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import io.v.v23.syncbase.nosql.PrefixRange;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CollectionBinding {
    public static class Builder extends BaseCollectionBindingBuilder<Builder> {
        public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> onPrefix(final String prefix) {
            return new PrefixBindingBuilder<T, A>(this).prefix(prefix);
        }
        public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> onPrefix(final PrefixRange prefix) {
            return new PrefixBindingBuilder<T, A>(this).prefix(prefix);
        }

        public <A extends RangeAdapter> IdListBindingBuilder<A> onIdList(final String idListRowName) {
            return new IdListBindingBuilder<A>(this).idListRowName(idListRowName);
        }
    }

    public static <A extends RangeAdapter> Builder builder() {
        return new Builder();
    }
}
