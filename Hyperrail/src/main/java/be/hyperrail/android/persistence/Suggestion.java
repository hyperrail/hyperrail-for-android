/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import be.hyperrail.android.irail.contracts.IrailRequest;

/**
 * A suggestion wraps a suggestable object together with the reason why it was suggested
 *
 * @param <T>
 */
public class Suggestion<T extends IrailRequest>  {

    private final T data;
    private final SuggestionType type;

    public Suggestion(T data, SuggestionType type) {

        this.data = data;
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public SuggestionType getType() {
        return type;
    }
}

