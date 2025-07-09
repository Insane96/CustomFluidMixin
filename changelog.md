# Changelog

## 1.6.1
* Fixed `block` result not working

## 1.6.0
* Added a `jei_only` field
  * If true the transformation will only show on JEI and not happen in the world  
    Useful if the transformation is already added by other mods and you want to add the mixin to JEI
  * Vanilla transformations are no longer hardcoded but included in the mod's data pack
* Renamed result type `blocks` to `block`
  * For a single block the field is now `block` instead of `blocks`

## 1.5.1
* Fixed crash with non `blocks` results

## 1.5.0
* Requires InsaneLib 1.15.0
* Changed `"result": "block"` to `"result": "blocks"`
  * `block` field has changed to `blocks`. Like before can be used as string specifying a block, or can be used as a list with weighted entries  
  This example makes the result have 50% chance to give stone and 50% to give cobblestone  
  ```json
    {
        "result": "blocks",
        "blocks": [
            {
                "block": "minecraft:stone",
                "weight": 1
            },
            {
                "block": "minecraft:cobblestone",
                "weight": 1
            }
        ]
    }
  ```

## 1.4.2
* Fixed possible loop when transforming a block into the same one
* Rewrote whole serialization of the JSON (could break basically)

## 1.4.1
* Update to MC 1.20.1
* Now compatible with InsaneLib 1.12.0

## 1.4.0
* MC 1.20