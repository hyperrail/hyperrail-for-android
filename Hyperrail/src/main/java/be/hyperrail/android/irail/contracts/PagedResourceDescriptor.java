/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.contracts;

/**
 * Describes where the previous, current and next page can be found
 */
public class PagedResourceDescriptor {
    private Object previousPointer, currentPointer, nextPointer;

    public PagedResourceDescriptor(Object previousPointer, Object currentPointer, Object nextPointer) {
        this.previousPointer = previousPointer;
        this.currentPointer = currentPointer;
        this.nextPointer = nextPointer;
    }

    public Object getPreviousPointer() {
        return previousPointer;
    }

    public Object getCurrentPointer() {
        return currentPointer;
    }

    public Object getNextPointer() {
        return nextPointer;
    }
}
