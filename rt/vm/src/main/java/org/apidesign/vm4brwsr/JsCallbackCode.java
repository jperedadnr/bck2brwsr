/**
 * Back 2 Browser Bytecode Translator
 * Copyright (C) 2012-2018 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://opensource.org/licenses/GPL-2.0.
 */
package org.apidesign.vm4brwsr;

import java.io.IOException;
import org.apidesign.vm4brwsr.ByteCodeParser.ClassData;
import org.apidesign.vm4brwsr.ByteCodeParser.MethodData;

final class JsCallbackCode extends LoopCode {
    private final MethodData m;
    private final StringBuilder sb;
    private Appendable dump;

    JsCallbackCode(ByteCodeToJavaScript b, Appendable out, NumberOperations n, ClassData jc, MethodData m) {
        this(b, new StringBuilder(), out, n, jc, m);
    }

    private JsCallbackCode(ByteCodeToJavaScript b, StringBuilder sb, Appendable out, NumberOperations n, ClassData jc, MethodData m) {
        super(b, sb, n, jc);
        this.m = m;
        this.sb = sb;
        this.dump = out;
    }

    @Override
    protected boolean beginCall(String[] mi, CharSequence[] vars, boolean isStatic) {
        if (dump != null && !isSpecialHtmlJavaCall(mi)) {
            sb.setLength(0);
            if (
                "current".equals(m.getName()) &&
                "<init>".equals(mi[1]) &&
                "(Lorg/netbeans/html/boot/spi/Fn$Presenter;)V".equals(mi[2])
            ) {
                vars[0] = vars[1] = "null";
            } else {
                if (!isStatic) {
                    vars[0] = "lcA1";
                }
            }
            sb.append("return ");
            return true;
        }
        return false;
    }

    @Override
    protected void endCall(boolean ok) throws IOException {
        if (ok) {
            dump.append(sb);
            dump = null;
        }
    }

    private static boolean isSpecialHtmlJavaCall(String[] mi) {
        return mi[0].startsWith("org/netbeans/html/boot/spi/Fn")
                || mi[0].startsWith("org/apidesign/html/boot/spi/Fn");
    }


}
