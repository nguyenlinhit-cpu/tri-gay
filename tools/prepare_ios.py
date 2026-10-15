#!/usr/bin/env python3
"""Export the Android level definitions and assets without executing Kotlin or third-party code."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import shutil
import struct
import wave

ROOT = Path(__file__).resolve().parents[1]
KOTLIN = ROOT / 'app/src/main/java/local/vn/suppermario'


def split_top(text, delimiter=','):
    out, start, depth, quoted, escaped = [], 0, 0, False, False
    for i, char in enumerate(text):
        if quoted:
            if escaped: escaped = False
            elif char == '\\': escaped = True
            elif char == '"': quoted = False
        elif char == '"': quoted = True
        elif char == '(': depth += 1
        elif char == ')': depth -= 1
        elif char == delimiter and depth == 0:
            out.append(text[start:i].strip())
            start = i + 1
    out.append(text[start:].strip())
    return [item for item in out if item]


FIELDS = {
    'PipeData': [('x', 0), ('height', 0), ('teleportTargetX', None)],
    'BlockData': [('x', 0), ('yOffset', 0), ('type', 'BRICK'), ('powerUpType', None), ('weaponType', None)],
    'CoinData': [('x', 0), ('yOffset', 0)],
    'GoombaData': [('x', 0), ('minX', 0), ('maxX', 0), ('speed', 0)],
    'BirdData': [('x', 0), ('minX', 0), ('maxX', 0), ('height', 350)],
    'RiverData': [('startX', 0), ('endX', 0)],
    'PlatformData': [('x', 0), ('yOffset', 0), ('width', 140), ('minX', 0), ('maxX', 0), ('speed', 0), ('isRock', False)],
    'LevelData': [('worldName', ''), ('skyColor', 0), ('timeLimit', 0), ('pipes', []), ('blocks', []),
                  ('coins', []), ('goombas', []), ('birds', []), ('rivers', []), ('platforms', []),
                  ('isBossLevel', False), ('bossX', 0), ('flagpoleX', 5200), ('challenge', ''), ('arenaStartX', 0)],
}
RULE_FIELDS = ['enemySpeed', 'hazardTempo', 'platformSpeed', 'timeScale', 'bossHp', 'bossShotInterval', 'bossShotSpeed']


def parse(expr, rules):
    expr = expr.strip().removesuffix('.toInt()')
    if expr.startswith('"'): return json.loads(expr)
    if expr in ('true', 'false'): return expr == 'true'
    if expr == 'null': return None
    if re.fullmatch(r'(?:PowerUpType|WeaponType|BlockType)\.\w+', expr): return expr.split('.')[1]
    if re.fullmatch(r'rules\.\w+', expr): return rules[expr.split('.')[1]]
    if re.fullmatch(r'0x[\dA-Fa-f]+', expr): return int(expr, 16)
    if re.fullmatch(r'-?\d+(?:\.\d+)?f?', expr): return float(expr.rstrip('f'))
    products = split_top(expr, '*')
    if len(products) > 1: return math.prod(parse(item, rules) for item in products)
    if re.fullmatch(r'[\d.f]+ to [\d.f]+', expr): return [parse(item, rules) for item in expr.split(' to ')]
    match = re.fullmatch(r'(\w+)\((.*)\)', expr, re.S)
    if not match: raise ValueError(f'Unsupported Kotlin expression: {expr}')
    name, body = match.groups()
    args = split_top(body)
    if name == 'listOf': return [parse(item, rules) for item in args]
    if name == 'time': return int(parse(args[0], rules) * rules['timeScale'])
    if name == 'trail':
        return [dict(zip(['x', 'yOffset'], parse(item, rules))) for item in args]
    if name == 'enemy':
        vals = [parse(item, rules) for item in args]
        vals[3] *= rules['enemySpeed']
        return dict(zip(['x', 'minX', 'maxX', 'speed'], vals))
    if name in ('supply', 'weapon'):
        return {'x': parse(args[0], rules), 'yOffset': 175, 'type': 'QUESTION',
                'powerUpType': parse(args[1], rules) if name == 'supply' else None,
                'weaponType': parse(args[1], rules) if name == 'weapon' else None}
    fields = FIELDS[name]
    result = {key: value for key, value in fields}
    for i, arg in enumerate(args):
        if '=' in arg:
            key, value = arg.split('=', 1)
            key = key.strip()
        else:
            key, value = fields[i][0], arg
        if key not in result: raise ValueError(key)
        result[key] = parse(value, rules)
    return result


def export_levels():
    source = re.sub(r'//[^\n]*', '', (KOTLIN / 'LevelData.kt').read_text())
    rules_source = (KOTLIN / 'ChallengeRules.kt').read_text()
    expressions = []
    for match in re.finditer(r'val level\d+ = (LevelData\()', source):
        start = match.start(1)
        depth, quote = 0, False
        for i in range(match.end(1) - 1, len(source)):
            char = source[i]
            if char == '"': quote = not quote
            if not quote:
                if char == '(': depth += 1
                if char == ')': depth -= 1
                if depth == 0:
                    expressions.append(source[start:i + 1])
                    break
    assert len(expressions) == 5
    result = {}
    for difficulty, values in re.findall(r'Difficulty\.(\w+) -> ChallengeRules\(([^)]+)\)', rules_source):
        rules = dict(zip(RULE_FIELDS, [parse(item, {}) for item in split_top(values)]))
        result[difficulty] = {'rules': rules, 'levels': [parse(expr, rules) for expr in expressions]}
    assert set(result) == {'EASY', 'MEDIUM', 'HARD'}
    return (json.dumps(result, ensure_ascii=False, indent=2) + '\n').encode()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true', help='Check committed fixtures match Android sources')
    args = parser.parse_args()
    data = export_levels()
    fixture = ROOT / 'ios/GameCore/Tests/GameCoreTests/Fixtures/levels.json'
    if args.check:
        assert fixture.read_bytes() == data, 'Regenerate iOS data with python3 tools/prepare_ios.py'
        print('iOS level data matches Android (5 levels × 3 modes).')
        return
    fixture.parent.mkdir(parents=True, exist_ok=True)
    fixture.write_bytes(data)
    resources = ROOT / 'ios/Resources'
    resources.mkdir(parents=True, exist_ok=True)
    (resources / 'levels.json').write_bytes(data)
    for image in (ROOT / 'app/src/main/res/drawable').glob('*.png'):
        shutil.copyfile(image, resources / image.name)
    # Small, original native PCM effects. AVAudioPlayer cannot load the Android Ogg files.
    for name, frequency in [('jump', 520), ('coin', 1050), ('hurt', 160), ('stomp', 260)]:
        sample_rate, seconds = 22050, 0.14
        count = int(sample_rate * seconds)
        samples = [int(8000 * (1 - i / count) ** 2 * math.sin(2 * math.pi * frequency * i / sample_rate)) for i in range(count)]
        with wave.open(str(resources / f'sfx_{name}.wav'), 'wb') as audio:
            audio.setparams((1, 2, sample_rate, 0, 'NONE', 'not compressed'))
            audio.writeframes(struct.pack(f'<{count}h', *samples))
    manifest = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(resources.iterdir()) if p.is_file() and p.name != 'manifest.json'}
    (resources / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(f'Prepared {len(manifest)} iOS resources; all 15 level variants exported from Kotlin.')


if __name__ == '__main__': main()
