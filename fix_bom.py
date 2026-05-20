import os

basedir = os.path.join(os.path.dirname(__file__), 'src', 'main', 'java')
testdir = os.path.join(os.path.dirname(__file__), 'src', 'test', 'java')
fixed = 0

for root, dirs, files in os.walk(basedir):
    for f in files:
        if not f.endswith('.java'):
            continue
        fp = os.path.join(root, f)
        with open(fp, 'rb') as fh:
            content = fh.read()
        if content[:3] == b'\xef\xbb\xbf':
            with open(fp, 'r', encoding='utf-8-sig') as fh:
                text = fh.read()
            with open(fp, 'w', encoding='utf-8') as fh:
                fh.write(text)
            fixed += 1
            print(f'Fixed BOM: {fp}')

for root, dirs, files in os.walk(testdir):
    for f in files:
        if not f.endswith('.java'):
            continue
        fp = os.path.join(root, f)
        with open(fp, 'rb') as fh:
            content = fh.read()
        if content[:3] == b'\xef\xbb\xbf':
            with open(fp, 'r', encoding='utf-8-sig') as fh:
                text = fh.read()
            with open(fp, 'w', encoding='utf-8') as fh:
                fh.write(text)
            fixed += 1
            print(f'Fixed BOM: {fp}')

print(f'Total files fixed: {fixed}')
