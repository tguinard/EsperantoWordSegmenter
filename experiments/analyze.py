#!/usr/bin/python3

import sys

# Is an Esperantilo morpheme the concatenation of multiple morphemes?
def is_still_correct(expect, result, esperantilo_roots):
    morph_expect = expect.split("'")
    morph_result = result.split("'")
    j = 0
    for i in range(len(morph_result)):
        if morph_result[i] == morph_expect[j]:
            j += 1
        elif morph_result[i] in esperantilo_roots:
            k = j + 1
            while k <= len(morph_expect) and len(''.join(morph_expect[j:k])) < len(morph_result[i]):
                k += 1
            if ''.join(morph_expect[j:k]) == morph_result[i]:
                j = k
            else:
                return False
        else:
            return False
    #print(expect, result)
    return True

#find number of errors, error rate by number of morphemes, total error rate
def find_real_errors(expect_list, result_list, esperantilo_roots):
    count = 0
    wrong = [0]
    total = [0]
    for expect, result in zip(expect_list, result_list):
        while expect.count("'") + 1 >= len(total):
            wrong.append(0)
            total.append(0)
        total[expect.count("'") + 1] += 1
        if expect != result and not is_still_correct(expect, result, esperantilo_roots):
            count += 1
            wrong[expect.count("'") + 1] += 1
    return count, [(i, 1 - wrong[i] / total[i]) for i in range(len(total)) if total[i] != 0], total, (sum(total) - count) / float(sum(total))

#find expected error if a random valid segmentation is selected
def find_random_output(expect_list, result_list, esperantilo_roots):
    count = 0
    wrong = [0]
    total = [0]
    for expect, results in zip(expect_list, result_list):
        while expect.count("'") + 1 >= len(total):
            wrong.append(0)
            total.append(0)
        total[expect.count("'") + 1] += 1
        wrongcount = 0
        for result in results:
            if expect != result and not is_still_correct(expect, result, esperantilo_roots):
                wrongcount += 1
        if len(results) == 0:
            wrong[expect.count("'") + 1] += 1
            count += 1
        else:
            wrong[expect.count("'") + 1] += wrongcount / float(len(results))
            count += wrongcount / float(len(results))
    return count, [(i, 1 - wrong[i] / total[i]) for i in range(len(total)) if total[i] != 0], total, (sum(total) - count) / float(sum(total))

def get_dissections(filename):
    #if 2+ segmentations are given, automatically counted as error
    return [''.join(line.split()[1:]) for line in open(filename)]

def get_dissections_random(filename):
    return [line.split() for line in open(filename)]

if len(sys.argv) < 3:
    print("Usage: %s expectFile resultsFile" % sys.argv[0])
    sys.exit()

expected = get_dissections(sys.argv[1])
esperantilo = set([word.strip() for word in open('extra_morphemes').readlines()])

if len(sys.argv) > 3 and sys.argv[3] == '-r':
    results = get_dissections_random(sys.argv[2])
    print(find_random_output(expected, results, esperantilo))
else:
    results = get_dissections(sys.argv[2])
    print(find_real_errors(expected, results, esperantilo))
