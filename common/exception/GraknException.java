/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package grakn.core.common.exception;

import java.util.List;

public class GraknException extends RuntimeException {

    public GraknException(final String error) {
        super(error);
    }

    public GraknException(final ErrorMessage error) {
        super(error.toString());
        assert !getMessage().contains("%s");
    }

    public GraknException(final Exception e) {
        super(e);
    }

    public GraknException(final List<GraknException> exceptions) {
        super(getMessages(exceptions));
    }

    public static GraknException of(final ErrorMessage errorMessage) {
        return new GraknException(errorMessage.message());
    }

    public static GraknException of(final String error) {
        return new GraknException(error);
    }

    public static String getMessages(final List<GraknException> exceptions) {
        final StringBuilder messages = new StringBuilder();
        for (GraknException exception : exceptions) {
            messages.append(exception.getMessage()).append("\n");
        }
        return messages.toString();
    }
}
