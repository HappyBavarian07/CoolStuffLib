import javalang
from pathlib import Path
from collections import defaultdict

def is_trivial_getter_setter(method):
    name = method.name
    if (name.startswith('get') or name.startswith('set')) and len(name) > 3 and name[3].isupper():
        if name.startswith('get') and len(method.parameters) == 0:
            return True
        if name.startswith('set') and len(method.parameters) == 1:
            return True
    return False

def is_override(method, class_decl, type_map):
    for path, node in class_decl:
        if isinstance(node, javalang.tree.ClassDeclaration):
            supers = []
            if node.extends:
                supers.append(node.extends.name)
            if node.implements:
                supers.extend([i.name for i in node.implements])
            for super_name in supers:
                if super_name in type_map:
                    super_type = type_map[super_name]
                    for m in super_type.methods:
                        if m.name == method.name and len(m.parameters) == len(method.parameters):
                            return True
    return False

def build_type_map(java_files):
    type_map = {}
    for java_file in java_files:
        with open(java_file, encoding='utf-8') as f:
            content = f.read()
        try:
            tree = javalang.parse.parse(content)
            for type_decl in tree.types:
                if isinstance(type_decl, javalang.tree.ClassDeclaration):
                    type_map[type_decl.name] = type_decl
        except Exception:
            continue
    return type_map

def has_javadoc(lines, method_line):
    idx = method_line - 2
    while idx >= 0:
        line = lines[idx].strip()
        if line == '':
            idx -= 1
            continue
        if line.startswith('/**'):
            end = method_line - 1
            start = idx
            javadoc_lines = lines[start:end]
            if len(javadoc_lines) == 1:
                return 'oneliner'
            return 'present'
        if line.startswith('//') or line.startswith('/*'):
            return None
        break
    return None

def main():
    src_root = Path('E:/InteliJ Programs/CoolStuffLib/src/main')
    java_files = list(src_root.rglob('*.java'))
    type_map = build_type_map(java_files)
    report = []
    class_counts = defaultdict(int)
    for java_file in java_files:
        with open(java_file, encoding='utf-8') as f:
            content = f.read()
        lines = content.splitlines()
        try:
            tree = javalang.parse.parse(content)
        except Exception:
            continue
        for path, node in tree:
            if isinstance(node, javalang.tree.ClassDeclaration):
                for method in node.methods:
                    if not (method.modifiers & {'public', 'protected'}):
                        continue
                    if is_trivial_getter_setter(method):
                        continue
                    if is_override(method, tree, type_map):
                        continue
                    method_line = method.position.line if method.position else None
                    if not method_line:
                        continue
                    javadoc_status = has_javadoc(lines, method_line)
                    if javadoc_status is None:
                        report.append(f'Missing Javadoc: {java_file}:{method_line} {method.name}')
                        class_name = Path(java_file).name
                        class_counts[class_name] += 1
                    elif javadoc_status == 'oneliner':
                        report.append(f'Unclear/One-liner Javadoc: {java_file}:{method_line} {method.name}')
                        class_name = Path(java_file).name
                        class_counts[class_name] += 1
    total = len(report)
    classes_affected = len(class_counts)
    with open('javadoc_report.txt', 'w', encoding='utf-8') as out:
        out.write('STATS:\n')
        out.write(f'Total missing Javadoc entries: {total}\n')
        out.write(f'Classes affected: {classes_affected}\n')
        out.write('Class breakdown:\n')
        for cls, count in sorted(class_counts.items(), key=lambda x: (-x[1], x[0])):
            out.write(f'{cls}: {count}\n')
        out.write('\n---\n\n')
        for line in report:
            out.write(line + '\n')
        out.write('\nFull paths with class names:\n')
        for java_file in java_files:
            class_name = Path(java_file).name
            out.write(f'{java_file} ({class_name})\n')
    print(f'Report written to javadoc_report.txt with {total} entries.')

if __name__ == '__main__':
    main()
