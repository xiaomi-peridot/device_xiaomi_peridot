#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.file import File
from extract_utils.fixups_blob import (
    BlobFixupCtx,
    blob_fixup,
    blob_fixups_user_type,
)

from extract_utils.fixups_lib import (
    lib_fixup_remove,
    lib_fixups,
    lib_fixups_user_type,
)

from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/xiaomi/peridot',
    'hardware/qcom-caf/sm8650',
    'hardware/xiaomi',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/qcom/opensource/commonsys/display',
]

blob_fixups: blob_fixups_user_type = {
    ('odm/etc/camera/enhance_motiontuning.xml',
     'odm/etc/camera/night_motiontuning.xml',
     'odm/etc/camera/motiontuning.xml'
    ): blob_fixup()
       .regex_replace('xml=version', 'xml version'),
    ('odm/lib64/hw/camera.qcom.so',
     'odm/lib64/hw/com.qti.chi.override.so',
     'odm/lib64/hw/camera.xiaomi.so',
     'odm/lib64/libcamxcommonutils.so',
     'odm/lib64/libmialgoengine.so',
     'odm/lib64/libchifeature2.so'): blob_fixup()
       .add_needed('libprocessgroup_shim.so'),
    'odm/lib64/libwrapper_dlengine.so': blob_fixup()
       .add_needed('libwrapper_dlengine_shim.so'),
    'system_ext/lib64/libwfdmmsrc_system.so': blob_fixup()
       .add_needed('libgui_shim.so'),
    'system_ext/lib64/libwfdnative.so': blob_fixup()
       .remove_needed('android.hidl.base@1.0.so')
       .add_needed('libbinder_shim.so')
       .add_needed('libinput_shim.so'),
    'system_ext/lib64/libwfdservice.so': blob_fixup()
       .replace_needed('android.media.audio.common.types-V2-cpp.so', 'android.media.audio.common.types-V3-cpp.so'),
    ('vendor/etc/media_codecs.xml',
     'vendor/etc/media_codecs_cliffs_v0.xml',
    ): blob_fixup()
       .regex_replace('.*media_codecs_(google_audio|google_c2|google_telephony|google_video|vendor_audio).*\n', ''),
    'vendor/etc/init/vendor.xiaomi.hardware.vibratorfeature.service.rc': blob_fixup()
       .regex_replace(r'/odm/bin/', r'/vendor/bin/'),
    'vendor/lib64/libqcodec2_core.so': blob_fixup()
        .add_needed('libcodec2_shim.so'),
    ('vendor/lib64/libqcrilNr.so',
     'vendor/lib64/libril-db.so'
    ): blob_fixup()
        .binary_regex_replace(rb'persist\.vendor\.radio\.poweron_opt', rb'persist.vendor.radio.poweron_ign'),
    'vendor/lib64/vendor.libdpmframework.so': blob_fixup()
        .add_needed('libhidlbase_shim.so'),
}  # fmt: skip

module = ExtractUtilsModule(
    'peridot',
    'xiaomi',
    blob_fixups=blob_fixups,
    namespace_imports=namespace_imports,
    add_firmware_proprietary_file=False,
    check_elf=False,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
