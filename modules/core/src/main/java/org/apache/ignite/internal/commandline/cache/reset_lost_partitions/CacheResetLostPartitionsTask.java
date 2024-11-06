/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ignite.internal.commandline.cache.reset_lost_partitions;

import org.apache.ignite.IgniteException;
import org.apache.ignite.internal.processors.cache.CacheGroupContext;
import org.apache.ignite.internal.processors.task.GridInternal;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.CU;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.visor.VisorJob;
import org.apache.ignite.internal.visor.VisorOneNodeTask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Reset status of lost partitions.
 */
@GridInternal
public class CacheResetLostPartitionsTask extends VisorOneNodeTask<CacheResetLostPartitionsTaskArg, CacheResetLostPartitionsTaskResult> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorJob<CacheResetLostPartitionsTaskArg, CacheResetLostPartitionsTaskResult> job(
        CacheResetLostPartitionsTaskArg arg) {
        return new CacheResetLostPartitionsJob(arg, debug);
    }

    /** Job for node. */
    private static class CacheResetLostPartitionsJob extends VisorJob<CacheResetLostPartitionsTaskArg, CacheResetLostPartitionsTaskResult> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Argument.
         * @param debug Debug.
         */
        public CacheResetLostPartitionsJob(@Nullable CacheResetLostPartitionsTaskArg arg, boolean debug) {
            super(arg, debug);
        }

        /** {@inheritDoc} */
        @Override public CacheResetLostPartitionsTaskResult run(
            CacheResetLostPartitionsTaskArg arg) throws IgniteException {
            try {
                final CacheResetLostPartitionsTaskResult res = new CacheResetLostPartitionsTaskResult();

                res.setMessageMap(new HashMap<>());

                if (!F.isEmpty(arg.getCaches())) {
                    Map<Integer, Set<String>> grpToCaches = new HashMap<>();
                    Map<Integer, String> grpToName = new HashMap<>();

                    for (String groupName : arg.getCaches()) {
                        int grpId = CU.cacheId(groupName);

                        CacheGroupContext grp = ignite.context().cache().cacheGroup(grpId);

                        if (grp != null) {
                            Set<String> grpCaches = new TreeSet<>();

                            grpToName.put(grpId, grp.cacheOrGroupName());
                            grpToCaches.put(grpId, grpCaches);

                            grp.caches().forEach(cctx -> grpCaches.add(cctx.name()));
                        }
                        else {
                            res.put(
                                groupName,
                                String.format("Cache group (name = '%s', id = %d) not found.",
                                groupName,
                                grpId));
                        }
                    }

                    if (!F.isEmpty(grpToCaches)) {
                        List<String> cachesToResetParts = grpToCaches
                            .values()
                            .stream()
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());

                        ignite.resetLostPartitions(cachesToResetParts);

                        grpToCaches.forEach((id, names) -> {
                            String grpName = grpToName.get(id);

                            res.put(
                                grpName,
                                String.format("Reset LOST-partitions performed successfully. " +
                                    "Cache group (name = '%s', id = %d), caches (%s).",
                                grpName,
                                id,
                                names));

                        });
                    }
                }

                return res;
            }
            catch (Exception e) {
                throw new IgniteException(e);
            }

        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(CacheResetLostPartitionsJob.class, this);
        }
    }
}
