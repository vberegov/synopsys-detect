/*
 * detectable
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detectable.detectables.yarn.workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import com.synopsys.integration.bdio.model.dependencyid.StringDependencyId;
import com.synopsys.integration.detectable.detectables.yarn.parse.YarnLockDependency;
import com.synopsys.integration.detectable.detectables.yarn.parse.entry.YarnLockEntry;

public class YarnWorkspaces {
    private final Collection<YarnWorkspace> workspaces;
    public static final YarnWorkspaces EMPTY = new YarnWorkspaces(new ArrayList<>(0));

    public YarnWorkspaces(Collection<YarnWorkspace> workspaces) {
        this.workspaces = workspaces;
    }

    public Collection<YarnWorkspace> getWorkspaces() {
        return workspaces;
    }

    public Optional<YarnWorkspace> lookup(YarnLockDependency yarnLockDependency) {
        return lookup(w -> w.matches(yarnLockDependency));
    }

    public Optional<YarnWorkspace> lookup(YarnLockEntry yarnLockEntry) {
        return lookup(w -> w.matches(yarnLockEntry));
    }

    public Optional<YarnWorkspace> lookup(String name, String version) {
        return lookup(w -> w.matches(name, version));
    }

    public Optional<YarnWorkspace> lookup(StringDependencyId dependencyId) {
        return lookup(w -> w.matches(dependencyId));
    }

    private Optional<YarnWorkspace> lookup(Predicate<YarnWorkspace> p) {
        for (YarnWorkspace candidateWorkspace : workspaces) {
            if (p.test(candidateWorkspace)) {
                return Optional.of(candidateWorkspace);
            }
        }
        return Optional.empty();
    }
}
