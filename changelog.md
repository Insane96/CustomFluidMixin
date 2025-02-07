# Changelog

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