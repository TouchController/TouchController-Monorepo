#!/bin/bash
set -e

if [ -z "$MODRINTH_TOKEN" ]
then
    echo "No MODRINTH_TOKEN set"
    exit 1
fi
if [ -z "$MODRINTH_PROJECT_ID" ]
then
    echo "No MODRINTH_PROJECT_ID set"
    exit 1
fi

./gradlew clean build

mod_name="$(grep modName gradle.properties | cut -d= -f2)"
mod_version="$(grep modVersion gradle.properties | cut -d= -f2)"
mod_state="$(grep modState gradle.properties | cut -d= -f2)"

function extract_changelog() {
    awk -v target_version="$1" '
         $0 ~ /^## / {
             if (found) exit
             if ($2 == target_version) found = 1
             next
         }
         found { print }
    ' "$2" | grep -v '^$'
}

changelog_en="$(extract_changelog "$mod_version" "NEWS-en.md")"
changelog_zh="$(extract_changelog "$mod_version" "NEWS-zh.md")"
changelog="$changelog_zh"$'\n\n---\n\n'"$changelog_en"

modrinth_versions="$(curl -sf "https://api.modrinth.com/v2/project/$MODRINTH_PROJECT_ID/version" -H "Authorization: $MODRINTH_TOKEN" | jq -r '.[].version_number')"
modmenu_versions="$(curl -sf "https://api.modrinth.com/v2/project/mOgUt4GM/version" -H "Authorization: $MODRINTH_TOKEN" | jq -cr '.[] | {version_number, id}')"
fabric_api_versions="$(curl -sf "https://api.modrinth.com/v2/project/P7dR8mSH/version" -H "Authorization: $MODRINTH_TOKEN" | jq -cr '.[] | {version_number, id}')"

function extract_version_id() {
    versions="$1"
    version_number="$2"
    grep -F "\"$version_number\"" <<< "$versions" | jq -r .id
}

mod_files=()

for module_dir in mod/*/{fabric,forge,neoforge}-*
do
    property_file="$module_dir/gradle.properties"
    function extract_property() {
        grep "$1" "$property_file" | cut -d= -f2
    }

    module_name="$(basename "$module_dir")"
    module_game_version="$(extract_property gameVersion)"
    module_loader="$(cut -d- -f1 <<< "$module_name")"
    version_id="$mod_version+$module_name"
    mod_file="$module_dir/build/libs/$mod_name-$mod_version+$module_name.jar"

    mod_files+=("$mod_file")

    function upload_modrinth() {
        if echo "$modrinth_versions" | grep -F "$version_id"
        then
            echo "Version $version_id is already on Modrinth, skip."
            return 0
        fi
        echo "Uploading $version_id to Modrinth"

        modrinth_dependencies="[]"
        if [ "$module_loader" == "fabric" ]
        then
            modmenu_version="$(extract_property modmenuVersion)"
            fabric_api_version="$(extract_property fabricApiVersion)"
            modmenu_id="$(extract_version_id "$modmenu_versions" "$modmenu_version")"
            fabric_api_id="$(extract_version_id "$fabric_api_versions" "$fabric_api_version")"
            modrinth_dependencies="$(jq -n                  \
                --arg modmenu_version "$modmenu_id"         \
                --arg fabric_api_version "$fabric_api_id"   \
                '[
                    {
                        "project_id": "mOgUt4GM",
                        "version_id": $modmenu_version,
                        "dependency_type": "optional"
                    },
                    {
                        "project_id": "P7dR8mSH",
                        "version_id": $fabric_api_version,
                        "dependency_type": "required"
                    }
                ]'
            )"
        fi
        modrinth_data="$(jq -n \
            --arg name "$module_game_version"           \
            --arg version_number "$version_id"          \
            --arg changelog "$changelog"                \
            --arg dependencies "$modrinth_dependencies" \
            --arg game_version "$module_game_version"   \
            --arg version_type "$mod_state"             \
            --arg loader "$module_loader"               \
            --arg project_id "$MODRINTH_PROJECT_ID"     \
            '{
                "name": $name,
                "version_number": $version_number,
                "changelog": $changelog,
                "dependencies": '"$modrinth_dependencies"',
                "game_versions": [$game_version],
                "version_type": $version_type,
                "loaders": [$loader],
                "project_id": $project_id,
                "file_parts": ["primary_file"],
                "primary_file": "primary_file"
            }'
        )"

        curl --fail-with-body "https://api.modrinth.com/v2/version" \
            -H "Authorization: $MODRINTH_TOKEN"                     \
            -F data="$modrinth_data"                                \
            -F primary_file="@$mod_file"
    }

    upload_modrinth

    # TODO: Upload CurseForge
done

bundle_file="bundle/TouchController-${mod_version}.zip"
rm -f "${bundle_file}"
zip -j "${bundle_file}" "${mod_files[@]}"

mkdir -p "release/${mod_version}"
for file in "${mod_files[@]}"
do
    name="$(basename "${file}")"
    cp "$file" "release/${mod_version}/${name}"
done
