// 分析烤全羊模型包围盒：从 bbmodel 原始空间 和 导出的 java 文件（渲染空间）
const fs = require('fs');
const path = require('path');

const DIR = __dirname;

function rotPoint(p, origin, rotDeg) {
    // 绕 origin 旋转（x,y,z 角度制）
    let [x, y, z] = [p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]];
    const rx = rotDeg[0] * Math.PI / 180, ry = rotDeg[1] * Math.PI / 180, rz = rotDeg[2] * Math.PI / 180;
    // 先绕X
    if (rx) {
        const c = Math.cos(rx), s = Math.sin(rx);
        const y1 = y * c - z * s, z1 = y * s + z * c;
        y = y1; z = z1;
    }
    // 绕Y
    if (ry) {
        const c = Math.cos(ry), s = Math.sin(ry);
        const x1 = x * c + z * s, z1 = -x * s + z * c;
        x = x1; z = z1;
    }
    // 绕Z
    if (rz) {
        const c = Math.cos(rz), s = Math.sin(rz);
        const x1 = x * c - y * s, y1 = x * s + y * c;
        x = x1; y = y1;
    }
    return [x + origin[0], y + origin[1], z + origin[2]];
}

// ============ 1. bbmodel 原始空间 ============
const bb = JSON.parse(fs.readFileSync(path.join(DIR, 'cooked_whole_sheep.bbmodel'), 'utf8'));
console.log('=== bbmodel meta ===');
console.log('name:', bb.name, '| flip_y:', bb.modded_entity_flip_y, '| visible_box:', JSON.stringify(bb.visible_box));

// outliner 层级
function printOutline(items, depth) {
    for (const it of items || []) {
        console.log(' '.repeat(depth * 2) + (it.name || '(unnamed)'));
        if (it.children && it.children.length) printOutline(it.children, depth + 1);
    }
}
if (bb.outliner) { console.log('=== OUTLINER ==='); printOutline(bb.outliner, 0); }

// 元素包围盒（原始空间，应用每个元素的旋转）
{
    const byName = {};
    let min = [1e9, 1e9, 1e9], max = [-1e9, -1e9, -1e9];
    for (const el of bb.elements) {
        const from = el.from, to = el.to;
        const rot = el.rotation || [0, 0, 0];
        const origin = el.origin || [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2];
        const name = el.name || '?';
        let emin = [1e9, 1e9, 1e9], emax = [-1e9, -1e9, -1e9];
        for (const corner of [[0,0,0],[1,0,0],[0,1,0],[0,0,1],[1,1,0],[1,0,1],[0,1,1],[1,1,1]]) {
            const p = rotPoint([from[0] + corner[0]*(to[0]-from[0]), from[1] + corner[1]*(to[1]-from[1]), from[2] + corner[2]*(to[2]-from[2])], origin, rot);
            for (let i = 0; i < 3; i++) { emin[i] = Math.min(emin[i], p[i]); emax[i] = Math.max(emax[i], p[i]); }
        }
        byName[name] = [emin, emax];
        for (let i = 0; i < 3; i++) { min[i] = Math.min(min[i], emin[i]); max[i] = Math.max(max[i], emax[i]); }
    }
    console.log('=== bbmodel RAW bbox (px) ===');
    console.log('x:', min[0].toFixed(2), '->', max[0].toFixed(2), ' width:', (max[0]-min[0]).toFixed(2));
    console.log('y:', min[1].toFixed(2), '->', max[1].toFixed(2), ' height:', (max[1]-min[1]).toFixed(2));
    console.log('z:', min[2].toFixed(2), '->', max[2].toFixed(2), ' depth:', (max[2]-min[2]).toFixed(2));
    console.log('width/16 blocks:', ((max[0]-min[0])/16).toFixed(2), ' height/16:', ((max[1]-min[1])/16).toFixed(2), ' depth/16:', ((max[2]-min[2])/16).toFixed(2));
    console.log('center x:', ((min[0]+max[0])/2).toFixed(2), ' y:', ((min[1]+max[1])/2).toFixed(2), ' z:', ((min[2]+max[2])/2).toFixed(2));
}

// ============ 2. 导出 java 渲染空间 ============
function parseJava(file) {
    const src = fs.readFileSync(path.join(DIR, file), 'utf8');
    // 提取 addOrReplaceChild 调用（含嵌套 addBox 与 PartPose）
    const parts = {}; // name -> {parent, cubes: [], pose: {x,y,z,rx,ry,rz}}
    const rootParts = [];
    const re = /(\w+)\.addOrReplaceChild\("(\w+)"\s*,\s*CubeListBuilder\.create\(\)([\s\S]*?),\s*PartPose\.(offset|offsetAndRotation|zero)\(([\s\S]*?)\)\s*\)/g;
    let m;
    while ((m = re.exec(src)) !== null) {
        const parentVar = m[1];
        const name = m[2];
        const builderBody = m[3];
        const poseType = m[4];
        const poseArgs = m[5];
        // cubes
        const cubes = [];
        const cubeRe = /\.addBox\((-?[\d.]+)F,\s*(-?[\d.]+)F,\s*(-?[\d.]+)F,\s*([\d.]+)F,\s*([\d.]+)F,\s*([\d.]+)F/g;
        let cm;
        while ((cm = cubeRe.exec(builderBody)) !== null) {
            cubes.push([parseFloat(cm[1]), parseFloat(cm[2]), parseFloat(cm[3]), parseFloat(cm[4]), parseFloat(cm[5]), parseFloat(cm[6])]);
        }
        let pose;
        if (poseType === 'zero') pose = { x: 0, y: 0, z: 0, rx: 0, ry: 0, rz: 0 };
        else {
            const nums = poseArgs.split(',').map(s => parseFloat(s.trim().replace('F', '')));
            pose = { x: nums[0], y: nums[1], z: nums[2], rx: nums[3] || 0, ry: nums[4] || 0, rz: nums[5] || 0 };
        }
        parts[name] = { parentVar, cubes, pose };
        if (parentVar === 'partdefinition') rootParts.push(name);
    }
    // 解析父变量名 -> 父 part 名
    const varToName = { 'partdefinition': 'ROOT' };
    for (const n in parts) {
        // 需要知道该 var 指向哪个 part。Blockbench 中变量名=part名（多数情况）
        if (parts[n].parentVar !== 'partdefinition') {
            // 父变量通常就是父 part 的名字（Blockbench 导出惯例）
        }
    }
    return { parts, rootParts };
}

// 计算每个 part 的绝对变换（y 旋转为主，支持全旋转，用矩阵）
function computeBBox(parsed) {
    const { parts, rootParts } = parsed;
    // 建立父映射：变量名 -> part 名
    // Blockbench 导出：变量名与 part 名一致，但为稳妥，用变量名直接匹配 part 名
    const varToPart = { 'partdefinition': null };
    for (const n in parts) {
        // 若变量名等于某个 part 名，则映射；否则忽略（子元素以变量名关联）
        if (parts[n].parentVar !== 'partdefinition' && parts[parts[n].parentVar]) {
            varToPart[parts[n].parentVar] = parts[n].parentVar;
        }
    }
    const bbox = [1e9, 1e9, 1e9, -1e9, -1e9, -1e9];
    const perPart = {};
    function walk(name, tx, ty, tz, rx, ry, rz) {
        // 当前累积旋转(rx,ry,rz) 与平移 (tx,ty,tz) 作用在子坐标系上
        const p = parts[name];
        if (!p) return;
        // 本 part 的变换：先平移 pose 再旋转 pose
        const nx = tx, ny = ty, nz = tz; // parent 累计
        // 子坐标系中的点 P_cube，经本 part 变换： R_pose * (P_cube) + pose_pos （在父坐标系）
        // 然后父级再应用累计变换。为简单，逐层递归时把“本 part 的世界变换”作为新的累计
        // 世界 = parentM * T(pose) * R(pose) * P_cube
        // 用矩阵相乘（3x3 旋转 + 平移）在像素单位
        const rad = [p.pose.rx, p.pose.ry, p.pose.rz];
        // 本 part 的局部旋转矩阵 R_local（先X后Y后Z，与 Minecraft rotationXYZ 一致）
        function rotMat(r) {
            const cx = Math.cos(r[0]), sx = Math.sin(r[0]);
            const cy = Math.cos(r[1]), sy = Math.sin(r[1]);
            const cz = Math.cos(r[2]), sz = Math.sin(r[2]);
            // JOML rotationXYZ = rotationX * rotationY * rotationZ（矩阵左乘顺序）
            // R = Rx * Ry * Rz
            const Rx = [[1,0,0],[0,cx,-sx],[0,sx,cx]];
            const Ry = [[cy,0,sy],[0,1,0],[-sy,0,cy]];
            const Rz = [[cz,-sz,0],[sz,cz,0],[0,0,1]];
            const Rxy = mul(Rx, Ry);
            return mul(Rxy, Rz);
        }
        function mul(A, B) {
            const C = [[0,0,0],[0,0,0],[0,0,0]];
            for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) {
                let s = 0;
                for (let k = 0; k < 3; k++) s += A[i][k] * B[k][j];
                C[i][j] = s;
            }
            return C;
        }
        // 本 part 在世界空间：M = parentM * T(pose) * R_local
        // 更新累计：newR = R_accum * R_local ; newT = R_accum * pose + T_accum
        const Rlocal = rotMat(rad);
        const newR = mul(rMat(rx, ry, rz), Rlocal);
        const pv = [p.pose.x, p.pose.y, p.pose.z];
        const t = [0,0,0];
        for (let i = 0; i < 3; i++) {
            t[i] = 0;
            for (let k = 0; k < 3; k++) t[i] += rMat(rx, ry, rz)[i][k] * pv[k];
        }
        const newT = [tx + t[0], ty + t[1], tz + t[2]];
        // 应用 newR/newT 到本 part 的所有 cube
        for (const c of p.cubes) {
            for (const corner of [[0,0,0],[1,0,0],[0,1,0],[0,0,1],[1,1,0],[1,0,1],[0,1,1],[1,1,1]]) {
                const px = c[0] + corner[0]*c[3], py = c[1] + corner[1]*c[4], pz = c[2] + corner[2]*c[5];
                let wx = 0, wy = 0, wz = 0;
                for (let i = 0; i < 3; i++) {
                    const v = [px, py, pz];
                    wx += newR[0][i] * v[i];
                    wy += newR[1][i] * v[i];
                    wz += newR[2][i] * v[i];
                }
                wx += newT[0]; wy += newT[1]; wz += newT[2];
                bbox[0] = Math.min(bbox[0], wx); bbox[1] = Math.min(bbox[1], wy); bbox[2] = Math.min(bbox[2], wz);
                bbox[3] = Math.max(bbox[3], wx); bbox[4] = Math.max(bbox[4], wy); bbox[5] = Math.max(bbox[5], wz);
            }
        }
        // 递归子 part
        for (const child in parts) {
            if (parts[child].parentVar === name) walk(child, newT[0], newT[1], newT[2], 0, 0, 0);
        }
    }
    // 根 part（parentVar === 'partdefinition'）
    for (const name of rootParts) walk(name, 0, 0, 0, 0, 0, 0);
    return bbox;
}

function rMat(rx, ry, rz) {
    const cx = Math.cos(rx), sx = Math.sin(rx);
    const cy = Math.cos(ry), sy = Math.sin(ry);
    const cz = Math.cos(rz), sz = Math.sin(rz);
    const Rx = [[1,0,0],[0,cx,-sx],[0,sx,cx]];
    const Ry = [[cy,0,sy],[0,1,0],[-sy,0,cy]];
    const Rz = [[cz,-sz,0],[sz,cz,0],[0,0,1]];
    const Rxy = [[0,0,0],[0,0,0],[0,0,0]];
    for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) {
        let s = 0; for (let k = 0; k < 3; k++) s += Rx[i][k] * Ry[k][j]; Rxy[i][j] = s;
    }
    const R = [[0,0,0],[0,0,0],[0,0,0]];
    for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) {
        let s = 0; for (let k = 0; k < 3; k++) s += Rxy[i][k] * Rz[k][j]; R[i][j] = s;
    }
    return R;
}

for (let stage = 0; stage <= 9; stage++) {
    const parsed = parseJava(`cooked_whole_sheep${stage}.java`);
    const b = computeBBox(parsed);
    const w = b[3]-b[0], h = b[4]-b[1], d = b[5]-b[2];
    const cx = (b[0]+b[3])/2, cy = (b[1]+b[4])/2, cz = (b[2]+b[5])/2;
    console.log(`=== export stage ${stage} bbox (px) ===`);
    console.log(`  x: ${b[0].toFixed(2)} -> ${b[3].toFixed(2)} (w=${w.toFixed(2)}, w/16=${(w/16).toFixed(2)}) center=${cx.toFixed(2)}`);
    console.log(`  y: ${b[1].toFixed(2)} -> ${b[4].toFixed(2)} (h=${h.toFixed(2)}, h/16=${(h/16).toFixed(2)}) center=${cy.toFixed(2)}`);
    console.log(`  z: ${b[2].toFixed(2)} -> ${b[5].toFixed(2)} (d=${d.toFixed(2)}, d/16=${(d/16).toFixed(2)}) center=${cz.toFixed(2)}`);
}
