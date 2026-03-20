import kotlin.test.Test
import kotlin.test.assertEquals

class FileStageDetectorTest {

    @Test
    fun happyPath() {
        val root = rootFolder("test-mod") {
            folder("Data")
        }

        val actual = detectStagingChanges(root)
        assertEquals(StageChange.NONE, actual)
    }

    @Test
    fun noFiles() {
        val root = mockFolder("test-mod")
        val actual = detectStagingChanges(root, arrayOf())
        assertEquals(StageChange.NO_FILES, actual)
    }

    @Test
    fun removeWinGDK() {
        val root = rootFolder("test-mod") {
            folder("Win64")
            folder("WinGDK")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.REMOVE_WINGDK, actual)
    }

    @Test
    fun detectFOMOD() {
        val root = rootFolder("test-mod") {
            folder("FOMOD")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.FOMOD, actual)
    }

    @Test
    fun capitalize() {
        val root = rootFolder("test-mod") {
            folder("data")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.CAPITALIZE, actual)
    }

    @Test
    fun nestInData() {
        val actuals = listOf("textures", "music", "sound", "meshes", "video").map { dataTopLevel ->
            val root = rootFolder("test-mod") {
                folder(dataTopLevel)
            }
            detectStagingChanges(root)
        }.toSet()
        assertEquals(1, actuals.size)
        assertEquals(StageChange.NEST_IN_DATA, actuals.first())
    }

    @Test
    fun nestInDataExtensions() {
        val actuals = listOf("esp", "esm", "ba2").map { dataTopLevel ->
            val root = rootFolder("test-mod") {
                file("thing.$dataTopLevel")
            }
            detectStagingChanges(root)
        }.toSet()
        assertEquals(1, actuals.size)
        assertEquals(StageChange.NEST_IN_DATA, actuals.first())
    }

    @Test
    fun useWin64FromObse64Prefix() {
        val root = rootFolder("test-mod") {
            file("obse64_loader.exe")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.USE_WIN64, actual)
    }

    @Test
    fun usePak() {
        val root = rootFolder("test-mod") {
            file("mod.pak")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.USE_PAK, actual)
    }

    @Test
    fun unnest() {
        val root = rootFolder("test-mod") {
            folder("wrapper") {
                folder("Data")
            }
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.UNNEST, actual)
    }

    @Test
    fun useWin64SingleDll() {
        val root = rootFolder("test-mod") {
            file("xinput.dll")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.USE_WIN64, actual)
    }

    @Test
    fun addTopFolder() {
        val root = rootFolder("test-mod") {
            file("enabled.txt")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.ADD_TOP_FOLDER, actual)
    }

    @Test
    fun removeTopFolderFromNestedExtension() {
        val root = rootFolder("test-mod") {
            folder("Data") {
                file("test.esp")
            }
        }

        val actual = detectStagingChanges(root)
        assertEquals(StageChange.REMOVE_TOP_FOLDER, actual)
    }

    @Test
    fun removeTopFolderFromNestedName() {
        val root = rootFolder("test-mod") {
            folder("Data") {
                file("engine")
            }
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.REMOVE_TOP_FOLDER, actual)
    }

    @Test
    fun removeTopFolderFallback() {
        val root = rootFolder("test-mod") {
            folder("wrapper") {
                file("readme.txt")
            }
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.REMOVE_TOP_FOLDER, actual)
    }

    @Test
    fun useUe4ss() {
        val root = rootFolder("test-mod") {
            folder("wrapper") {
                file("enabled.txt")
            }
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.USE_UE4SS, actual)
    }

    @Test
    fun useScriptExtenderFromObsePluginsPath() {
        val root = rootFolder("test-mod") {
            folder("obse") {
                folder("plugins") {
                    file("plugin.dll")
                }
            }
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.USE_SCRIPT_EXTENDER, actual)
    }

    @Test
    fun useScriptExtenderFromSfsePluginsPath() {
        val root = rootFolder("test-mod") {
            folder("sfse") {
                folder("plugins") {
                    file("plugin.dll")
                }
            }
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.USE_SCRIPT_EXTENDER, actual)
    }

    @Test
    fun useWin64FromDwmapiAndUe4ss() {
        val root = rootFolder("test-mod") {
            file("dwmapi.dll")
            file("ue4ss.dll")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.USE_WIN64, actual)
    }

    @Test
    fun capitalizeUe4ssMods() {
        val root = rootFolder("test-mod") {
            folder("ue4ss") {
                folder("Mods")
            }
            file("readme.txt")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.CAPITALIZE, actual)
    }

    @Test
    fun noneForEngineTopLevelFile() {
        val root = rootFolder("test-mod") {
            file("engine.ini")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.NONE, actual)
    }
}
