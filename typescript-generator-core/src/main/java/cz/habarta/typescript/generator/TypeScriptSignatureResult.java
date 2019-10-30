/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import java.util.Collections;
import java.util.List;

/**
 * Returned by the static method specified by {@link TypeScriptSignatureViaStaticMethod}.
 */
public interface TypeScriptSignatureResult {

    /** @return The custom TypeScript signature to output verbatim for this element. */
    String signature();

    /** @return Any classes the generator should processes as if they were 'discovered' in this element. */
    default List<Class<?>> additionalClassesToProcess() { return Collections.emptyList(); }

}
