/*
 * MinecraftDecompiler. A tool/library to deobfuscate and decompile jars.
 * Copyright (C) 2019-2024 MaxPixelStudios(XiaoPangxie732)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.maxpixel.mcdecompiler.mapping;

import org.jetbrains.annotations.NotNull;

/**
 * Intended to be a universal interface for getting names
 */
public interface NameGetter {
    String getUnmappedName();

    String getMappedName();

    /**
     * For {@link NamespacedMapping} to implement.
     *
     * @implNote unmapped namespace should be set by mapping processors
     */
    interface Namespaced {
        String getUnmappedNamespace();

        String getMappedNamespace();

        void setMappedNamespace(@NotNull String namespace);
    }
}