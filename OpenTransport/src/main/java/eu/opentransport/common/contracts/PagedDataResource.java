/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.common.contracts;

/**
 * A paged resource contains pointers to the next and previous pages.
 * Pointers are API-implementation dependent
 */
public interface PagedDataResource {
    PagedDataResourceDescriptor getPagedResourceDescriptor();

    void setPageInfo(PagedDataResourceDescriptor descriptor);
}
