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

package org.apidesign.bck2brwsr.mojo;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class UtilBaseTest {

    public UtilBaseTest() {
    }

    @Test
    public void testConvertDefaultPage() throws Exception {
        String data = "\n"
            + "<html>\n" +
            "    <head>\n" +
            "    </head>\n" +
            "    <body>\n" +
            "        <h1>H1</h1>\n" +
            "<!-- ${browser.bootstrap} -->\n" +
            "    </body>\n" +
            "</html>";

        String res = UtilBase.augmentedIndexPage(data, "main.js", "org.apidesign.demo.Test");

        assertNotEquals(res.indexOf("loadClass('org.apidesign.demo.Test'"), -1, "loadClass found");
        assertNotEquals(res.indexOf("script src='bck2brwsr.js'>"), -1, "init vm found");

        String res2 = UtilBase.augmentedIndexPage(data, "main.js", "org.apidesign.demo.Test");
        assertEquals(res2, res, "No change then");
    }
}
